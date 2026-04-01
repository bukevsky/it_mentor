import { defineStore } from "pinia";
import { computed, ref } from "vue";
import type {
  CityResponse,
  ErrorResponse,
  InteractionTypeResponse,
  LanguageResponse,
  SkillResponse
} from "@/shared/api/contracts";
import { normalizeErrorResponse } from "@/shared/lib/api-errors";
import { dictionariesApi } from "../api/dictionaries-api";

export const useDictionariesStore = defineStore("dictionaries", () => {
  const cities = ref<CityResponse[]>([]);
  const skills = ref<SkillResponse[]>([]);
  const languages = ref<LanguageResponse[]>([]);
  const interactionTypes = ref<InteractionTypeResponse[]>([]);
  const isLoading = ref(false);
  const error = ref<ErrorResponse | null>(null);
  const isLoaded = computed(() => {
    return Boolean(
      cities.value.length || skills.value.length || languages.value.length || interactionTypes.value.length
    );
  });

  const loadAll = async () => {
    isLoading.value = true;
    error.value = null;

    try {
      const [nextCities, nextSkills, nextLanguages, nextInteractionTypes] = await Promise.all([
        dictionariesApi.getCities(),
        dictionariesApi.getSkills(),
        dictionariesApi.getLanguages(),
        dictionariesApi.getInteractionTypes()
      ]);

      cities.value = nextCities;
      skills.value = nextSkills;
      languages.value = nextLanguages;
      interactionTypes.value = nextInteractionTypes;
    } catch (rawError) {
      error.value = normalizeErrorResponse(rawError, "/dictionaries");
    } finally {
      isLoading.value = false;
    }
  };

  return {
    cities,
    error,
    interactionTypes,
    isLoaded,
    isLoading,
    languages,
    skills,
    loadAll
  };
});
