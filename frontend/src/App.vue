<template>
  <div class="app-shell">
    <header class="topbar">
      <div>
        <h1>AI 招聘匹配系统</h1>
        <p>简历解析、JD 建模、候选人范围筛选和雷达图评估</p>
      </div>
      <span class="status-pill">当前 JD：{{ currentJobId ? `#${currentJobId}` : '未选择' }}</span>
    </header>

    <main class="workspace">
      <aside class="sidebar">
        <section class="panel">
          <div class="panel-heading">
            <h2>岗位 JD</h2>
            <button class="link-button" @click="loadJobs">刷新</button>
          </div>

          <div class="segmented">
            <button :class="{ active: jobMode === 'existing' }" @click="jobMode = 'existing'">选择已有</button>
            <button :class="{ active: jobMode === 'new' }" @click="jobMode = 'new'">新建 JD</button>
          </div>

          <div v-if="jobMode === 'existing'">
            <label for="jobSelect">已创建 JD</label>
            <select id="jobSelect" v-model="currentJobId" class="input" @change="onJobChange">
              <option :value="null" disabled>请选择 JD</option>
              <option v-for="job in jobs" :key="job.id" :value="job.id">
                #{{ job.id }} {{ job.title }}
              </option>
            </select>
            <p class="message" :class="{ error: messages.jobError }">{{ messages.jobError || selectedJobSummary }}</p>
          </div>

          <div v-else>
            <label for="jobTitle">岗位名称</label>
            <input id="jobTitle" v-model.trim="jobForm.title" class="input" placeholder="例如：Java 后端工程师">

            <label for="jdText">JD 内容</label>
            <textarea id="jdText" v-model.trim="jobForm.jdText" class="textarea" placeholder="粘贴岗位职责、必备技能、经验年限和薪资范围"></textarea>

            <button class="primary-button" :disabled="loading.job || !canCreateJob" @click="createJob">
              {{ loading.job ? '创建中...' : '创建 JD' }}
            </button>
            <p class="message" :class="{ error: messages.jobError }">{{ messages.jobError || messages.job }}</p>
          </div>
        </section>

        <section class="panel">
          <div class="panel-heading">
            <h2>简历库</h2>
            <button class="link-button" @click="loadResumes">刷新</button>
          </div>

          <label for="resumeFile">上传新简历</label>
          <input id="resumeFile" ref="resumeInput" class="input file-input" type="file" accept=".pdf,.doc,.docx,.txt,image/*" @change="onFileChange">

          <button class="primary-button" :disabled="loading.resume || !resumeFile" @click="uploadResume">
            {{ loading.resume ? '解析中...' : '上传并解析' }}
          </button>
          <p class="message" :class="{ error: messages.resumeError }">{{ messages.resumeError || messages.resume }}</p>

          <div class="list-actions">
            <span>已选 {{ selectedResumeIds.length }} / {{ resumes.length }}</span>
            <div>
              <button class="link-button" @click="selectAllResumes">全选</button>
              <button class="link-button" @click="selectedResumeIds = []">清空</button>
            </div>
          </div>

          <div class="resume-list">
            <label v-for="resume in resumes" :key="resume.id" class="resume-item">
              <input v-model="selectedResumeIds" type="checkbox" :value="resume.id">
              <span>
                <strong>#{{ resume.id }} {{ resume.candidateName || '未知候选人' }}</strong>
                <small>{{ resume.originalFilename || resume.email || resume.phone || '暂无文件名' }}</small>
              </span>
            </label>
          </div>
        </section>

        <section class="panel">
          <h2>推荐排序</h2>
          <button class="primary-button" :disabled="loading.match || !canRecommend" @click="recommend">
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
            <span>推荐结果</span>
            <strong>{{ results.length }}</strong>
          </div>
          <div class="metric">
            <span>最高匹配分</span>
            <strong>{{ topScore }}</strong>
          </div>
          <div class="metric">
            <span>简历库总数</span>
            <strong>{{ resumes.length }}</strong>
          </div>
        </div>

        <div class="content-grid">
          <div v-if="results.length" class="table-wrap">
            <table>
              <thead>
              <tr>
                <th>排名</th>
                <th>候选人</th>
                <th>总分</th>
                <th>技能</th>
                <th>经验</th>
                <th>项目</th>
                <th>匹配原因</th>
                <th>短板</th>
                <th>原简历</th>
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
                <td>
                  <strong>#{{ item.resumeId }} {{ item.resume?.candidateName || '未知候选人' }}</strong>
                  <small>{{ item.resume?.originalFilename || item.resume?.email || '' }}</small>
                </td>
                <td><span class="score">{{ item.totalScore }}</span></td>
                <td>{{ item.skillScore }}</td>
                <td>{{ item.experienceScore }}</td>
                <td>{{ item.projectScore }}</td>
                <td>{{ item.matchReason }}</td>
                <td>{{ item.weaknessReason }}</td>
                <td>
                  <button class="table-button" @click.stop="openResume(item.resumeId)">打开</button>
                </td>
              </tr>
              </tbody>
            </table>
          </div>

          <div v-else class="empty-state">
            <div>
              <strong>暂无匹配结果</strong>
              <p>选择 JD 和候选简历后生成推荐排序。</p>
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

const jobMode = ref('existing');
const jobs = ref([]);
const resumes = ref([]);
const currentJobId = ref(null);
const selectedResumeIds = ref([]);
const resumeFile = ref(null);
const resumeInput = ref(null);
const results = ref([]);
const selectedResult = ref(null);
const radarEl = ref(null);
let chart = null;

const loading = reactive({
  job: false,
  resume: false,
  match: false,
  jobs: false,
  resumes: false
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
const canRecommend = computed(() => currentJobId.value && selectedResumeIds.value.length > 0);
const topScore = computed(() => results.value.length ? results.value[0].totalScore : '-');
const selectedJobSummary = computed(() => {
  const job = jobs.value.find(item => item.id === currentJobId.value);
  return job ? job.summary || `已选择 ${job.title}` : '请选择一个已创建的 JD';
});

onMounted(async () => {
  window.addEventListener('resize', resizeRadar);
  await Promise.all([loadJobs(), loadResumes()]);
  await nextTick();
  initRadar();
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeRadar);
  disposeRadar();
});

async function loadJobs() {
  loading.jobs = true;
  messages.jobError = '';
  try {
    jobs.value = await request('/api/jobs');
    if (!currentJobId.value && jobs.value.length) {
      currentJobId.value = jobs.value[0].id;
    }
  } catch (error) {
    messages.jobError = error.message;
  } finally {
    loading.jobs = false;
  }
}

async function loadResumes() {
  loading.resumes = true;
  messages.resumeError = '';
  try {
    resumes.value = await request('/api/resumes');
  } catch (error) {
    messages.resumeError = error.message;
  } finally {
    loading.resumes = false;
  }
}

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
    jobMode.value = 'existing';
    clearResults();
    await loadJobs();
    currentJobId.value = data.id;
    messages.job = `JD 创建成功，岗位 ID：#${data.id}`;
  } catch (error) {
    messages.jobError = error.message;
  } finally {
    loading.job = false;
  }
}

function onJobChange() {
  clearResults();
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
    resumeFile.value = null;
    if (resumeInput.value) {
      resumeInput.value.value = '';
    }
    await loadResumes();
    if (!selectedResumeIds.value.includes(data.id)) {
      selectedResumeIds.value = [...selectedResumeIds.value, data.id];
    }
    const name = data.profile?.name || '未知候选人';
    messages.resume = `简历解析成功，简历 ID：#${data.id}，候选人：${name}`;
  } catch (error) {
    messages.resumeError = error.message;
  } finally {
    loading.resume = false;
  }
}

function selectAllResumes() {
  selectedResumeIds.value = resumes.value.map(resume => resume.id);
}

async function recommend() {
  loading.match = true;
  messages.match = '';
  messages.matchError = '';
  try {
    const params = new URLSearchParams();
    selectedResumeIds.value.forEach(id => params.append('resumeIds', id));
    const data = await request(`/api/matches/jobs/${currentJobId.value}/recommendations?${params}`);
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

function openResume(resumeId) {
  window.open(`/api/resumes/${resumeId}/file`, '_blank', 'noopener');
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
