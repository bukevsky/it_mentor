type QueryValue = string | number | boolean | null | undefined;
type QueryArray = Array<string | number | boolean>;

export type QueryParams = Record<string, QueryValue | QueryArray>;

export const buildQuery = (params: QueryParams) => {
  const searchParams = new URLSearchParams();

  Object.entries(params).forEach(([key, rawValue]) => {
    if (rawValue === undefined || rawValue === null || rawValue === "") {
      return;
    }

    if (Array.isArray(rawValue)) {
      rawValue.forEach((value) => {
        searchParams.append(key, String(value));
      });
      return;
    }

    searchParams.set(key, String(rawValue));
  });

  const query = searchParams.toString();
  return query ? `?${query}` : "";
};
