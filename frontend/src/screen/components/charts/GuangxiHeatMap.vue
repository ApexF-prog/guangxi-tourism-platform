<template>
  <div ref="chartRef" class="chart"></div>
</template>

<script setup>
import guangxiJson from "@screen/assets/map/guangxi.json";
import { useChart, echarts } from "@screen/composables/useChart";
import { computed } from "vue";
import { data, cityIndex, totalHeat, flowLines } from "@screen/data/tourism";

const MAP_NAME = "guangxi";
const heatValues = computed(() => data.cityHeat.map((c) => c.value));
const heatMin = computed(() => Math.floor(Math.min(...heatValues.value) * 0.9 / 100) * 100);
const heatMax = computed(() => Math.ceil(Math.max(...heatValues.value) * 1.02 / 100) * 100);

const { chartRef } = useChart(
  () => ({
    tooltip: {
      trigger: "item",
      backgroundColor: "rgba(3, 18, 36, 0.85)",
      borderColor: "#38e8ff",
      borderWidth: 1,
      textStyle: { color: "#fff", fontSize: 12 },
      formatter: (params) => {
        if (params.seriesType === "effectScatter") {
          const d = params.data;
          return `<b style="color:#f2c14e">${d.name}</b> ${d.star}<br/>
              所属城市：${d.city}<br/>热度指数：<b>${d.value[2]}</b><br/>
              门票价格：<b>${d.price > 0 ? d.price + " 元" : "免费开放"}</b>`;
        }
        if (params.seriesType === "lines") return "";
        const c = cityIndex.value[params.name];
        if (!c) return params.name;
        return `<b style="color:#38e8ff">${c.name}</b><br/>
            接待游客：<b>${c.value.toLocaleString()}</b> 万人次<br/>
            在监测景区：<b>${c.spots}</b> 个<br/>
            占全区 ${((c.value / totalHeat.value) * 100).toFixed(1)}%`;
      },
    },

    visualMap: {
      type: "continuous",
      seriesIndex: 0,
      min: heatMin.value,
      max: heatMax.value,
      left: "2%",
      bottom: "4%",
      itemWidth: 9,
      itemHeight: 80,
      calculable: true,
      text: ["热度高", "热度低"],
      textStyle: { color: "#cbe4f7", fontSize: 10 },
      inRange: { color: ["#0b2f4f", "#0f5d80", "#1189a6", "#22c1b8", "#f2c14e"] },
    },

    geo: {
      map: MAP_NAME,
      roam: true,
      zoom: 1.05,
      scaleLimit: { min: 0.85, max: 3 },
      layoutCenter: ["50%", "50%"],
      layoutSize: "96%",
      label: {
        show: true,
        color: "#cbe4f7",
        fontSize: 10,
        textBorderColor: "rgba(3, 13, 26, 0.9)",
        textBorderWidth: 3,
      },
      emphasis: { label: { color: "#ffffff", fontSize: 13 } },
      itemStyle: {
        areaColor: "#0a2540",
        borderColor: "rgba(94, 214, 255, 0.55)",
        borderWidth: 1,
      },
    },

    series: [
      { name: "城市", type: "map", geoIndex: 0, data: data.cityHeat },
      {
        name: "热门景区",
        type: "effectScatter",
        coordinateSystem: "geo",
        z: 3,
        symbolSize: (val) => 4.5 + (val[2] - 55) / 5.4,
        rippleEffect: { brushType: "stroke", scale: 3, period: 3.6 },
        itemStyle: {
          color: "#f2c14e",
          shadowColor: "rgba(242, 193, 78, 0.9)",
          shadowBlur: 12,
        },
        label: { show: false },
        emphasis: {
          label: {
            show: true,
            position: "right",
            formatter: "{b}",
            color: "#fff",
            fontSize: 11,
            backgroundColor: "rgba(6, 22, 40, 0.85)",
            padding: [3, 6],
            borderRadius: 2,
          },
        },
        data: data.scenicSpots.map((s) => ({
          name: s.name,
          value: [...s.coord, s.heat],
          city: s.city,
          price: s.price,
          star: s.star,
        })),
      },
      {
        name: "客源流向",
        type: "lines",
        coordinateSystem: "geo",
        z: 2,
        silent: true,
        effect: {
          show: true,
          period: 4.5,
          trailLength: 0.18,
          symbol: "circle",
          symbolSize: 3,
          color: "#9bf6ff",
        },
        lineStyle: {
          color: "#38e8ff",
          width: 0.9,
          opacity: 0.3,
          curveness: 0.22,
        },
        data: flowLines.value,
      },
    ],
  }),
  {
    beforeInit: () => echarts.registerMap(MAP_NAME, guangxiJson),
    refreshKey: () => data.version,
  },
);
</script>
