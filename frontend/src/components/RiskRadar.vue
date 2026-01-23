<script setup lang="ts">
import * as echarts from 'echarts';
import { onMounted, onBeforeUnmount, ref, watch } from 'vue';

const props = defineProps<{
  info: number;
  finance: number;
  psych: number;
}>();

const chartEl = ref<HTMLDivElement | null>(null);
let chart: echarts.ECharts | null = null;

const init = () => {
  if (!chartEl.value) return;
  chart = echarts.init(chartEl.value);
  chart.setOption({
    tooltip: {},
    radar: {
      indicator: [
        { name: '信息防护', max: 100 },
        { name: '金融安全', max: 100 },
        { name: '心理防线', max: 100 },
      ],
    },
    series: [
      {
        type: 'radar',
        data: [
          {
            value: [props.info, props.finance, props.psych],
            areaStyle: { opacity: 0.2 },
          },
        ],
      },
    ],
  });
};

onMounted(init);
watch(() => [props.info, props.finance, props.psych], init);
onBeforeUnmount(() => {
  chart?.dispose();
});
</script>

<template>
  <div ref="chartEl" style="width: 100%; height: 320px;"></div>
</template>
