export const formatJson = (value: unknown) => JSON.stringify(value, null, 2);

export const parseJsonInput = <T>(value: string): T => {
  return JSON.parse(value) as T;
};

export const parseNumberList = (value: string) => {
  return value
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean)
    .map((item) => Number(item))
    .filter((item) => Number.isFinite(item));
};

export const parseOptionalNumber = (value: string) => {
  const normalized = value.trim();

  if (!normalized) {
    return undefined;
  }

  const parsed = Number(normalized);
  return Number.isFinite(parsed) ? parsed : undefined;
};
