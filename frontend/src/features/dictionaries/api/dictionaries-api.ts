import type {
  CityResponse,
  InteractionTypeResponse,
  LanguageResponse,
  SkillResponse
} from "@/shared/api/contracts";
import { request } from "@/shared/api/http";

export const dictionariesApi = {
  getCities() {
    return request<CityResponse[]>("/dictionaries/cities", { auth: false });
  },
  getSkills() {
    return request<SkillResponse[]>("/dictionaries/skills", { auth: false });
  },
  getLanguages() {
    return request<LanguageResponse[]>("/dictionaries/languages", { auth: false });
  },
  getInteractionTypes() {
    return request<InteractionTypeResponse[]>("/dictionaries/interaction-types", { auth: false });
  }
};
