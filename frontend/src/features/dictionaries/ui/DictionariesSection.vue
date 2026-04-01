<script setup lang="ts">
import { storeToRefs } from "pinia";
import JsonPreview from "@/shared/ui/JsonPreview.vue";
import SectionCard from "@/shared/ui/SectionCard.vue";
import { useDictionariesStore } from "../model/dictionaries-store";

const dictionariesStore = useDictionariesStore();
const { cities, error, interactionTypes, isLoaded, isLoading, languages, skills } =
  storeToRefs(dictionariesStore);
</script>

<template>
  <SectionCard
    description="Публичные справочники из backend guide. Загружаются без токена и используются как look-up для профилей и поиска."
    kicker="Dictionaries"
    title="Справочники"
  >
    <div class="actions">
      <button class="primary-button" :disabled="isLoading" type="button" @click="dictionariesStore.loadAll">
        {{ isLoading ? "Загрузка..." : "GET /dictionaries/**" }}
      </button>
      <p class="hint">{{ isLoaded ? "Справочники загружены" : "Справочники еще не загружались" }}</p>
    </div>

    <div v-if="error" class="error-box">
      <strong>{{ error.error }}</strong>
      <p>{{ error.message }}</p>
    </div>

    <div class="section-grid">
      <JsonPreview :value="cities" title="Cities" />
      <JsonPreview :value="skills" title="Skills" />
      <JsonPreview :value="languages" title="Languages" />
      <JsonPreview :value="interactionTypes" title="Interaction Types" />
    </div>
  </SectionCard>
</template>
