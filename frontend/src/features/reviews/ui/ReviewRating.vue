<script setup lang="ts">
import { computed, ref } from "vue";
import { BaseIcon } from "conductor";

const props = withDefaults(
  defineProps<{
    modelValue?: number;
    value?: number;
    readonly?: boolean;
    showNumber?: boolean;
  }>(),
  {
    modelValue: 0,
    value: 0,
    readonly: false,
    showNumber: true
  }
);

const emit = defineEmits<{
  (event: "update:modelValue", value: number): void;
}>();

const hoveredRating = ref(0);
const currentRating = computed(() => props.value || props.modelValue);
const displayedRating = computed(() =>
  !props.readonly && hoveredRating.value ? hoveredRating.value : Math.round(currentRating.value)
);

const getStarClass = (star: number) => ({
  "review-rating__star--active": star <= displayedRating.value,
  "review-rating__star--button": !props.readonly
});

const setRating = (star: number) => {
  if (!props.readonly) {
    emit("update:modelValue", star);
  }
};
</script>

<template>
  <div class="review-rating" :class="{ 'review-rating--interactive': !readonly }">
    <button
      v-for="star in 5"
      :key="star"
      class="review-rating__star"
      :class="getStarClass(star)"
      type="button"
      :disabled="readonly"
      :aria-label="`${star} из 5`"
      @mouseenter="hoveredRating = readonly ? 0 : star"
      @focus="hoveredRating = readonly ? 0 : star"
      @mouseleave="hoveredRating = 0"
      @blur="hoveredRating = 0"
      @click="setRating(star)"
    >
      <BaseIcon icon="star" :width="18" :height="18" />
    </button>
    <strong v-if="showNumber" class="review-rating__value">
      {{ (value || modelValue).toFixed(1) }}
    </strong>
  </div>
</template>
