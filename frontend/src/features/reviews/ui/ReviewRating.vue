<script setup lang="ts">
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

const getStarClass = (star: number) => ({
  "review-rating__star--active": star <= Math.round(props.value || props.modelValue),
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
      @click="setRating(star)"
    >
      ★
    </button>
    <strong v-if="showNumber" class="review-rating__value">
      {{ (value || modelValue).toFixed(1) }}
    </strong>
  </div>
</template>
