<script setup lang="ts">
import AppLayout from '../components/AppLayout.vue'
import SimulationPanel from '../components/SimulationPanel.vue'
</script>

<template>
  <AppLayout>
    <div class="simulation-page">
      <div class="page-header">
        <h2>JSONata 在线仿真 Playground</h2>
      </div>
      <el-card>
        <SimulationPanel />
      </el-card>
    </div>
  </AppLayout>
</template>

<style scoped>
.simulation-page {
  max-width: 1200px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 18px;
}
</style>
