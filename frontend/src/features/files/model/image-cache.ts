import { fetchFileBlob } from "@/features/files/api/files-api";

interface ImageCacheEntry {
  blob: Blob;
  filename: string;
  url: string;
}

const cache = new Map<number, ImageCacheEntry>();
const pending = new Map<number, Promise<string>>();
const queuedLoads: Array<() => void> = [];
let activeLoads = 0;
const MAX_PARALLEL_IMAGE_LOADS = 2;
const MAX_IMAGE_CACHE_ITEMS = 40;

const createDownload = (entry: ImageCacheEntry, fallbackFilename: string) => {
  const link = document.createElement("a");
  link.href = URL.createObjectURL(entry.blob);
  link.download = entry.filename || fallbackFilename;
  link.rel = "noopener";
  link.style.display = "none";
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.setTimeout(() => URL.revokeObjectURL(link.href), 1000);
};

const runQueued = <T>(task: () => Promise<T>): Promise<T> =>
  new Promise((resolve, reject) => {
    const start = () => {
      activeLoads += 1;
      task()
        .then(resolve)
        .catch(reject)
        .finally(() => {
          activeLoads -= 1;
          queuedLoads.shift()?.();
        });
    };

    if (activeLoads < MAX_PARALLEL_IMAGE_LOADS) {
      start();
    } else {
      queuedLoads.push(start);
    }
  });

export const imageCache = {
  get(fileId: number): string | undefined {
    return cache.get(fileId)?.url;
  },

  prime(fileId: number, blob: Blob, filename: string): string {
    const hit = cache.get(fileId);
    if (hit) {
      return hit.url;
    }

    const url = URL.createObjectURL(blob);
    if (cache.size >= MAX_IMAGE_CACHE_ITEMS) {
      const oldest = cache.keys().next().value as number | undefined;
      if (oldest !== undefined) {
        const entry = cache.get(oldest);
        if (entry) {
          URL.revokeObjectURL(entry.url);
        }
        cache.delete(oldest);
      }
    }
    cache.set(fileId, { blob, filename, url });
    return url;
  },

  load(fileId: number, fallbackFilename = "image"): Promise<string> {
    const hit = cache.get(fileId)?.url;
    if (hit) return Promise.resolve(hit);

    const inFlight = pending.get(fileId);
    if (inFlight) return inFlight;

    const promise = runQueued(() => fetchFileBlob(fileId, fallbackFilename))
      .then((file) => {
        const url = imageCache.prime(fileId, file.blob, file.filename);
        pending.delete(fileId);
        return url;
      })
      .catch((err: unknown) => {
        pending.delete(fileId);
        throw err;
      });

    pending.set(fileId, promise);
    return promise;
  },

  download(fileId: number, fallbackFilename: string): boolean {
    const hit = cache.get(fileId);
    if (!hit) {
      return false;
    }

    createDownload(hit, fallbackFilename);
    return true;
  }
};

const imageExtensions = new Set(["jpg", "jpeg", "png", "gif", "webp", "bmp", "avif"]);

export const isImageAttachment = (contentType: string | null | undefined, filename = "") => {
  if (contentType?.toLowerCase().startsWith("image/")) {
    return true;
  }

  const extension = filename.split(".").pop()?.toLowerCase() ?? "";
  return imageExtensions.has(extension);
};
