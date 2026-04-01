const TOKEN_STORAGE_KEY = "it-mentor.access-token";

let inMemoryToken: string | null = null;

const getStorage = (): Storage | null => {
  try {
    return window.sessionStorage;
  } catch {
    return null;
  }
};

export const tokenStorage = {
  get(): string | null {
    return getStorage()?.getItem(TOKEN_STORAGE_KEY) ?? inMemoryToken;
  },
  set(value: string): void {
    inMemoryToken = value;
    getStorage()?.setItem(TOKEN_STORAGE_KEY, value);
  },
  clear(): void {
    inMemoryToken = null;
    getStorage()?.removeItem(TOKEN_STORAGE_KEY);
  }
};
