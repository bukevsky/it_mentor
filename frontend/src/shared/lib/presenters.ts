import type { Option } from "@/shared/lib/options";

export const getOptionLabel = <T extends string>(
  options: readonly Option<T>[],
  value?: string | null,
  fallback = "—"
) => {
  if (!value) {
    return fallback;
  }

  return options.find((option) => option.value === value)?.label ?? value;
};

export const formatDateTime = (value?: string | null) => {
  if (!value) {
    return "—";
  }

  return new Intl.DateTimeFormat("ru-RU", {
    dateStyle: "medium",
    timeStyle: "short"
  }).format(new Date(value));
};

export const formatList = (items: Array<string | null | undefined>, fallback = "—") => {
  const normalized = items.filter((item): item is string => Boolean(item?.trim()));
  return normalized.length ? normalized.join(", ") : fallback;
};

export const fullName = (person: {
  firstName: string;
  lastName: string;
  middleName?: string | null;
}) => {
  return [person.lastName, person.firstName, person.middleName].filter(Boolean).join(" ");
};
