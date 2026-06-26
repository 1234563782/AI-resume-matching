<template>
  <div class="app-shell">
    <header class="topbar">
      <div>
        <h1>AI 招聘匹配系统</h1>
        <p>简历解析、JD 建模、匹配排序和雷达图评估</p>
      </div>
      <span class="status-pill">当前 JD：{{ currentJobId ? `#${currentJobId}` : '未创建' }}</span>
    </header>

    <main class="workspace">
      <aside class="sidebar">
        <section class="panel">
          <h2>创建岗位 JD</h2>
          <label for="jobTitle">岗位名称</label>
          <input id="jobTitle" v-model.trim="jobForm.title" class="input" placeholder="例如：Java 后端工程师">

          <label for="jdText">JD 内容</label>
          <textarea id="jdText" v-model.trim="jobForm.jdText" class="textarea" placeholder="粘贴岗位职责、必备技能、经验年限和薪资范围"></textarea>

          <button class="primary-button" :disabled="loading.job || !canCreateJob" @click="createJob">
            {{ loading.job ? '创建中...' : '创建 JD' }}
          </button>
          <p class="message" :class="{ error: messages.jobError }">{{ messages.jobError || messages.job }}</p>
        </section>

        <section class="panel">
          <h2>上传候选人简历</h2>
          <label for="resumeFile">简历文件</label>
          <input id="resumeFile" ref="resumeInput" class="input file-input" type="file" accept=".pdf,.doc,.docx,.txt,image/*" @change="onFileChange">

          <button class="primary-button" :disabled="loading.resume || !resumeFile" @click="uploadResume">
            {{ loading.resume ? '解析中...' : '上传并解析' }}
          </button>
          <p class="message" :class="{ error: messages.resumeError }">{{ messages.resumeError || messages.resume }}</p>
        </section>

        <section class="panel">
          <h2>推荐排序</h2>
          <button class="primary-button" :disabled="loading.match || !currentJobId" @click="recommend">
            {{ loading.match ? '计算中...' : '生成候选人推荐' }}
          </button>
          <div class="inline-actions">
            <button class="secondary-button" @click="clearResults">清空结果</button>
            <button class="secondary-button" :disabled="!selectedResult" @click="renderSelectedRadar">刷新雷达图</button>
          </div>
          <p class="message" :class="{ error: messages.matchError }">{{ messages.matchError || messages.match }}</p>
        </section>
      </aside>

      <section class="main-panel">
        <div class="metrics-grid">
          <div class="metric">
            <span>候选人数</span>
            <strong>{{ results.length }}</strong>
          </div>
          <div class="metric">
            <span>最高匹配分</span>
            <strong>{{ topScore }}</strong>
          </div>
          <div class="metric">
            <span>已解析简历</span>
            <strong>{{ uploadedResumeCount }}</strong>
          </div>
        </div>

        <div class="content-grid">
          <div v-if="results.length" class="table-wrap">
            <table>
              <thead>
              <tr>
                <th>排名</th>
                <th>简历 ID</th>
                <th>总分</th>
                <th>技能</th>
                <th>经验</th>
                <th>项目</th>
                <th>匹配原因</th>
                <th>短板</th>
              </tr>
              </thead>
              <tbody>
              <tr
                v-for="(item, index) in results"
                :key="`${item.resumeId}-${item.jobId}`"
                :class="{ selected: selectedResult && selectedResult.resumeId === item.resumeId }"
                @click="selectResult(item)"
              >
                <td>{{ index + 1 }}</td>
                <td>#{{ item.resumeId }}</td>
                <td><span class="score">{{ item.totalScore }}</span></td>
                <td>{{ item.skillScore }}</td>
                <td>{{ item.experienceScore }}</td>
                <td>{{ item.projectScore }}</td>
                <td>{{ item.matchReason }}</td>
                <td>{{ item.weaknessReason }}</td>
              </tr>
              </tbody>
            </table>
          </div>

          <div v-else class="empty-state">
            <div>
              <strong>暂无匹配结果</strong>
              <p>创建 JD、上传简历后生成推荐排序。</p>
            </div>
          </div>

          <div class="radar-box">
            <div class="radar-header">
              <h2>匹配雷达图</h2>
              <span>{{ selectedResult ? `简历 #${selectedResult.resumeId}` : '未选择候选人' }}</span>
            </div>
            <div ref="radarEl" class="radar-chart"></div>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import * as echarts from 'echarts';

const jobForm = reactive({
  title: 'Java 后端工程师',
  jdText: '负责 Java 后端服务开发，要求熟悉 Spring Boot、MySQL、Redis，有 3 年以上业务系统开发经验。'
});

const currentJobId = ref(null);
const resumeFile = ref(null);
const resumeInput = ref(null);
const uploadedResumeCount = ref(0);
const results = ref([]);
const selectedResult = ref(null);
const radarEl = ref(null);
let chart = null;

const loading = reactive({
  job: false,
  resume: false,
  match: false
});

const messages = reactive({
  job: '',
  resume: '',
  match: '',
  jobError: '',
  resumeError: '',
  matchError: ''
});

const canCreateJob = computed(() => jobForm.title.length > 0 && jobForm.jdText.length > 0);
const topScore = computed(() => results.value.length ? results.value[0].totalScore : '-');

onMounted(() => {
  window.addEventListener('resize', resizeRadar);
  nextTick(initRadar);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeRadar);
  disposeRadar();
});

async function createJob() {
  loading.job = true;
  messages.job = '';
  messages.jobError = '';
  try {
    const data = await request('/api/jobs', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(jobForm)
    });
    currentJobId.value = data.id;
    clearResults();
    messages.job = `JD 创建成功，岗位 ID：#${data.id}`;
  } catch (error) {
    messages.jobError = error.message;
  } finally {
    loading.job = false;
  }
}

function onFileChange(event) {
  resumeFile.value = event.target.files[0] || null;
  messages.resumeError = '';
  messages.resume = resumeFile.value ? `已选择：${resumeFile.value.name}` : '';
}

async function uploadResume() {
  loading.resume = true;
  messages.resumeError = '';
  try {
    const formData = new FormData();
    formData.append('file', resumeFile.value);
    const data = await request('/api/resumes', {
      method: 'POST',
      body: formData
    });
    uploadedResumeCount.value += 1;
    resumeFile.value = null;
    if (resumeInput.value) {
      resumeInput.value.value = '';
    }
    const name = data.profile?.name || '未知候选人';
    messages.resume = `简历解析成功，简历 ID：#${data.id}，候选人：${name}`;
  } catch (error) {
    messages.resumeError = error.message;
  } finally {
    loading.resume = false;
  }
}

async function recommend() {
  loading.match = true;
  messages.match = '';
  messages.matchError = '';
  try {
    const data = await request(`/api/matches/jobs/${currentJobId.value}/recommendations`);
    results.value = Array.isArray(data) ? data : [];
    selectedResult.value = results.value[0] || null;
    messages.match = results.value.length ? `已生成 ${results.value.length} 条推荐结果` : '当前没有可推荐的简历';
    await nextTick();
    renderSelectedRadar();
  } catch (error) {
    messages.matchError = error.message;
  } finally {
    loading.match = false;
  }
}

function selectResult(result) {
  selectedResult.value = result;
  renderSelectedRadar();
}

function clearResults() {
  results.value = [];
  selectedResult.value = null;
  messages.match = '';
  renderRadar(null);
}

function initRadar() {
  if (!radarEl.value) {
    return;
  }
  disposeRadar();
  chart = echarts.init(radarEl.value);
  renderRadar(selectedResult.value);
}

function renderSelectedRadar() {
  renderRadar(selectedResult.value);
}

function renderRadar(result) {
  if (!radarEl.value) {
    return;
  }
  if (!chart) {
    chart = echarts.init(radarEl.value);
  }

  const radar = result?.radar || {};
  const hasResult = Boolean(result);
  chart.setOption({
    tooltip: {},
    radar: {
      radius: '64%',
      indicator: [
        { name: '技能', max: 100 },
        { name: '经验', max: 100 },
        { name: '教育', max: 100 },
        { name: '项目', max: 100 },
        { name: '薪资', max: 100 },
        { name: '语义', max: 100 }
      ],
      splitArea: { areaStyle: { color: ['#f8fafc', '#ffffff'] } }
    },
    series: [{
      type: 'radar',
      areaStyle: { opacity: hasResult ? 0.18 : 0 },
      lineStyle: { width: hasResult ? 2 : 0 },
      data: [{
        name: hasResult ? `简历 #${result.resumeId}` : '暂无数据',
        value: [
          radar.skill || 0,
          radar.experience || 0,
          radar.education || 0,
          radar.project || 0,
          radar.salary || 0,
          radar.semantic || 0
        ],
        itemStyle: { opacity: hasResult ? 1 : 0 }
      }]
    }]
  }, true);
  requestAnimationFrame(resizeRadar);
}

function resizeRadar() {
  if (chart) {
    chart.resize();
  }
}

function disposeRadar() {
  if (chart) {
    chart.dispose();
    chart = null;
  }
}

async function request(url, options = {}) {
  const response = await fetch(url, options);
  const contentType = response.headers.get('content-type') || '';
  const body = contentType.includes('application/json') ? await response.json() : await response.text();
  if (!response.ok) {
    throw new Error(typeof body === 'string' ? body : body.message || '请求失败');
  }
  return body;
}
</script>
