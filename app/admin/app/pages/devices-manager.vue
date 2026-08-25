<template>
  <div class="container-admin py-8 sm:py-10">
    <!-- 标题 -->
    <header class="mb-6 flex flex-wrap items-end justify-between gap-3">
      <div>
        <h1 class="text-2xl font-semibold tracking-tight">机型管理</h1>
        <p class="mt-1 text-sm text-[var(--color-text-secondary)]">
          以大陆正式版为基准查看机型信息，支持编辑；同机型其余分支按 branches 表顺序列出
        </p>
      </div>
      <NuxtLink to="/" class="text-xs font-medium text-[var(--color-text-tertiary)] transition-colors hover:text-[var(--color-text)]">
        ← 返回仪表盘
      </NuxtLink>
    </header>

    <!-- 机型选择 -->
    <div class="card p-4">
      <div class="relative">
        <svg
          class="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-[var(--color-text-tertiary)]"
          xmlns="http://www.w3.org/2000/svg"
          fill="none"
          viewBox="0 0 24 24"
          stroke-width="1.5"
          stroke="currentColor"
          aria-hidden="true"
        >
          <path stroke-linecap="round" stroke-linejoin="round" d="m21 21-5.197-5.197m0 0A7.5 7.5 0 1 0 5.196 5.196a7.5 7.5 0 0 0 10.607 10.607Z" />
        </svg>
        <input
          v-model="keyword"
          type="search"
          placeholder="搜索并选择机型（设备代号或名称）"
          class="input-base pl-10"
          @focus="listOpen = true"
          @input="listOpen = true"
          @blur="onBlur"
        />
        <Transition
          enter-active-class="transition duration-150 ease-out"
          enter-from-class="opacity-0 -translate-y-1"
          enter-to-class="opacity-100 translate-y-0"
          leave-active-class="transition duration-100 ease-in"
          leave-from-class="opacity-100"
          leave-to-class="opacity-0"
        >
          <ul
            v-if="listOpen"
            class="absolute z-20 mt-2 max-h-72 w-full overflow-y-auto rounded-lg border border-[var(--color-border)] bg-[var(--color-bg-surface)] shadow-xl"
            role="listbox"
            aria-label="机型列表"
          >
            <li
              v-for="d in filtered"
              :key="d.device"
              role="option"
              class="cursor-pointer px-4 py-2.5 transition-colors hover:bg-[var(--color-bg-subtle)]"
              @mousedown.prevent="select(d)"
            >
              <div class="flex items-center justify-between gap-3">
                <span class="truncate text-sm font-medium text-[var(--color-text)]">
                  {{ d.name }}
                  <span class="ml-1.5 font-mono text-xs font-normal text-[var(--color-text-tertiary)]">{{ d.device }}</span>
                </span>
                <span class="badge shrink-0 border border-[var(--color-border)] text-[var(--color-text-tertiary)]">{{ d.count }} 分支</span>
              </div>
              <p v-if="d.brands" class="mt-0.5 truncate text-xs text-[var(--color-text-tertiary)]">{{ d.brands }}</p>
            </li>
            <li v-if="filtered.length === 0" class="px-4 py-6 text-center text-xs text-[var(--color-text-tertiary)]">
              没有匹配的机型
            </li>
          </ul>
        </Transition>
      </div>

      <!-- 当前选中 -->
      <div v-if="selected" class="mt-3 flex flex-wrap items-center gap-2 text-sm">
        <span class="text-[var(--color-text-tertiary)]">已选择：</span>
        <span class="font-semibold text-[var(--color-text)]">{{ detail?.baseline?.name || selected }}</span>
        <span class="font-mono text-xs text-[var(--color-text-tertiary)]">{{ selected }}</span>
        <button type="button" class="btn-secondary !px-2.5 !py-1 text-xs" @click="clear">清除</button>
      </div>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="flex justify-center py-24">
      <span class="spinner" role="status" aria-label="加载中"></span>
    </div>

    <!-- 错误 -->
    <div v-else-if="loadError" class="card mt-6 px-6 py-12 text-center">
      <p class="text-sm text-[var(--color-text-secondary)]">机型详情加载失败</p>
      <p class="mt-1 break-all font-mono text-xs text-[var(--color-danger)]">{{ loadError }}</p>
      <button type="button" class="btn-secondary mt-4" @click="selected ? loadDetail(selected) : reloadDevices">重试</button>
    </div>

    <!-- 未选择 -->
    <div v-else-if="!detail" class="card mt-6 px-6 py-16 text-center">
      <p class="text-sm text-[var(--color-text-tertiary)]">请在上方搜索并选择一个机型查看详情，或者新增机型</p>
      <button type="button" class="btn-primary mt-4 !py-1.5 text-xs" @click="openCreateDevice">
        <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" aria-hidden="true">
          <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
        </svg>
        新增机型
      </button>
    </div>

    <!-- 新增机型弹窗 -->
    <Teleport to="body">
      <Transition
        enter-active-class="transition duration-200 ease-out"
        enter-from-class="opacity-0"
        enter-to-class="opacity-100"
        leave-active-class="transition duration-150 ease-in"
        leave-from-class="opacity-100"
        leave-to-class="opacity-0"
      >
        <div v-if="createOpen" class="fixed inset-0 z-[90] overflow-y-auto" role="dialog" aria-modal="true" aria-label="新增机型">
          <div class="absolute inset-0 bg-black/40 backdrop-blur-sm" @click="createOpen = false"></div>
          <div class="relative mx-auto my-8 w-full max-w-2xl rounded-xl border border-[var(--color-border)] bg-[var(--color-bg-surface)] p-6 shadow-xl">
            <div class="mb-4 flex items-start justify-between gap-4">
              <div>
                <h3 class="text-base font-semibold text-[var(--color-text)]">新增机型</h3>
                <p class="mt-0.5 text-xs text-[var(--color-text-secondary)]">
                  粘贴 ROM 文件名并点击提取，自动填充设备信息后确认创建
                </p>
              </div>
              <button
                type="button"
                class="flex h-8 w-8 shrink-0 items-center justify-center rounded-md text-[var(--color-text-tertiary)] transition-colors hover:bg-[var(--color-bg-subtle)] hover:text-[var(--color-text)]"
                aria-label="关闭"
                @click="createOpen = false"
              >
                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" aria-hidden="true">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M6 18 18 6M6 6l12 12" />
                </svg>
              </button>
            </div>

            <div class="space-y-4">
              <!-- ROM 文件名识别 -->
              <div>
                <label for="create-filename" class="form-label">
                  ROM 文件名 <span class="text-xs font-normal text-[var(--color-text-tertiary)]">粘贴文件名后点击提取</span>
                </label>
                <div class="flex gap-2">
                  <input
                    id="create-filename"
                    v-model="createFilename"
                    type="text"
                    class="input-base flex-1 font-mono text-sm"
                    placeholder="如 houji-ota_full-OS3.0.305.0.WNCCNXM-user-16.0-c7f619bf84.zip"
                  />
                  <button
                    type="button"
                    class="btn-primary shrink-0 !px-4 !py-1.5 text-xs"
                    :disabled="!createFilename.trim()"
                    @click="onCreateExtract"
                  >
                    提取
                  </button>
                </div>
                <p v-if="createDetected" class="mt-1 text-xs text-green-600 dark:text-green-400">
                  已识别：tag={{ createDetected.tag }} · region={{ createDetected.region }} · devcode={{ createDetected.devcode }}
                  <span v-if="createDetected.carrier"> · carrier={{ createDetected.carrier }}</span>
                </p>
              </div>
              <div>
                <label for="create-device" class="form-label">设备代号 <span class="text-[var(--color-danger)]">*</span></label>
                <input
                  id="create-device"
                  v-model="createDeviceName"
                  type="text"
                  class="input-base font-mono text-sm"
                  placeholder="如 beryl、marble"
                  @keyup.enter="doCreateDevice"
                />
                <p class="mt-1 text-xs text-[var(--color-text-tertiary)]">英文小写，与已有代号不可重复</p>
              </div>
              <div>
                <label for="create-devtag" class="form-label">devtag <span class="text-xs font-normal text-[var(--color-text-tertiary)]">设备内部标识</span></label>
                <input
                  id="create-devtag"
                  v-model="createDevtag"
                  type="text"
                  class="input-base font-mono text-sm"
                  placeholder="如 OQ、MA（可留空）"
                />
              </div>
              <div>
                <label for="create-devcode" class="form-label">devcode <span class="text-xs font-normal text-[var(--color-text-tertiary)]">设备版本号后6位</span></label>
                <input
                  id="create-devcode"
                  v-model="createDevcode"
                  type="text"
                  class="input-base font-mono text-sm"
                  placeholder="如 OQCNXM（可留空）"
                />
              </div>
            </div>

            <div class="mt-6 flex justify-end gap-2">
              <button type="button" class="btn-secondary" @click="createOpen = false">取消</button>
              <button
                type="button"
                class="btn-primary"
                :disabled="!createDeviceName.trim() || creating"
                @click="doCreateDevice"
              >
                {{ creating ? '创建中…' : '创建' }}
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <template v-if="detail">
      <!-- 基准信息（设备全局信息，保存后同步所有分支） -->
      <section class="card mt-6 overflow-hidden">
        <div class="flex flex-wrap items-start justify-between gap-3 border-b border-[var(--color-border)] px-5 py-4">
          <div>
            <h2 class="text-sm font-semibold text-[var(--color-text)]">设备全局信息</h2>
            <p class="mt-0.5 text-xs text-[var(--color-text-secondary)]">
              编辑后保存，同步至该机型全部 {{ detail.branches.length }} 个分支
            </p>
          </div>
          <span class="badge border border-[var(--color-border)] font-mono text-[var(--color-text-secondary)]">
            {{ detail.baseline.device }}
          </span>
        </div>

        <div class="px-5 py-5">
          <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
            <!-- device -->
            <div>
              <label class="form-label !mb-1">
                device <span class="text-xs font-normal text-[var(--color-text-tertiary)]">设备代号</span>
              </label>
              <input
                v-model="globalDevice"
                type="text"
                class="input-base font-mono text-sm"
                placeholder="如 houji、marble"
              />
            </div>
            <!-- devtag -->
            <div>
              <label class="form-label !mb-1">
                devtag <span class="text-xs font-normal text-[var(--color-text-tertiary)]">设备内部标识</span>
              </label>
              <input
                v-model="globalDevtag"
                type="text"
                class="input-base font-mono text-sm"
                placeholder="如 OQ、MA（可留空）"
              />
            </div>
            <!-- internal -->
            <div>
              <label class="form-label !mb-1">
                internal <span class="text-xs font-normal text-[var(--color-text-tertiary)]">设备内部标识</span>
              </label>
              <input
                v-model="globalInternal"
                type="text"
                class="input-base font-mono text-sm"
                placeholder="可留空"
              />
            </div>
            <!-- full_brands -->
            <div>
              <label class="form-label !mb-1">
                full_brands <span class="text-xs font-normal text-[var(--color-text-tertiary)]">品牌全称，JSON 数组</span>
              </label>
              <textarea
                v-model="globalFullBrands"
                rows="2"
                class="input-base font-mono text-xs"
                placeholder='如 ["Xiaomi","Redmi","POCO"]'
                spellcheck="false"
              ></textarea>
              <button type="button" class="btn-secondary mt-1.5 !py-1 text-xs" @click="genGlobalFullBrands">
                从分支生成
              </button>
            </div>
            <!-- full_names -->
            <div>
              <label class="form-label !mb-1">
                full_names <span class="text-xs font-normal text-[var(--color-text-tertiary)]">设备全名，所有分支名称总和</span>
              </label>
              <div class="grid grid-cols-2 gap-2">
                <div>
                  <label class="text-xs text-[var(--color-text-tertiary)]">中文</label>
                  <input v-model="globalFullNamesZh" type="text" class="input-base font-mono text-sm" placeholder="中文名称" />
                </div>
                <div>
                  <label class="text-xs text-[var(--color-text-tertiary)]">English</label>
                  <input v-model="globalFullNamesEn" type="text" class="input-base font-mono text-sm" placeholder="English name" />
                </div>
              </div>
              <button type="button" class="btn-secondary mt-1.5 !py-1 text-xs" @click="genGlobalFullNames">
                从分支生成
              </button>
            </div>
          </div>
          <div class="mt-4 flex justify-end">
            <button
              type="button"
              class="btn-primary !py-1.5 text-xs"
              :disabled="globalSaving || !globalDirty"
              @click="saveGlobalFields"
            >
              {{ globalSaving ? '保存中…' : '保存并同步全部分支' }}
            </button>
          </div>
        </div>
      </section>

      <!-- 全部分支 -->
      <section class="card mt-6 overflow-hidden">
        <div class="flex flex-wrap items-center justify-between gap-3 border-b border-[var(--color-border)] px-5 py-4">
          <div>
            <h2 class="text-sm font-semibold text-[var(--color-text)]">全部分支（{{ detail.branches.length }}）</h2>
            <p class="mt-0.5 text-xs text-[var(--color-text-secondary)]">按 branches 表顺序排列；点击行可编辑</p>
          </div>
          <button
            type="button"
            class="btn-primary !py-1.5 text-xs"
            :disabled="!detail.availableBranches || detail.availableBranches.length === 0"
            @click="openBranchPicker"
          >
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" aria-hidden="true">
              <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
            </svg>
            新增分支
          </button>
        </div>

        <div class="overflow-x-auto">
          <table class="table-base">
            <thead>
              <tr class="border-b border-[var(--color-border)] bg-[var(--color-bg-subtle)]">
                <th class="table-th">分支</th>
                <th class="table-th">tag</th>
                <th class="table-th">code</th>
                <th class="table-th">region</th>
                <th class="table-th">devtag</th>
                <th class="table-th">branchcode</th>
                <th class="table-th">状态</th>
                <th class="table-th">操作</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-[var(--color-border)]">
              <tr
                v-for="b in detail.branches"
                :key="b.id"
                class="group cursor-pointer transition-colors hover:bg-[var(--color-bg-subtle)]"
                :class="b.isBaseline && 'bg-[var(--color-bg-subtle)]'"
                @click="openBranchEdit(b)"
              >
                <td class="table-td">
                  <p class="text-xs font-medium text-[var(--color-text)]">
                    <span v-if="b.isBaseline" class="mr-1.5 inline-block rounded bg-[var(--color-primary)]/10 px-1.5 py-0.5 text-[10px] font-semibold text-[var(--color-primary)]">基准</span>
                    {{ b.branchNameZh || b.tag || '—' }}
                  </p>
                  <p v-if="b.branchNameEn" class="text-xs text-[var(--color-text-tertiary)]">{{ b.branchNameEn }}</p>
                </td>
                <td class="table-td whitespace-nowrap font-mono text-xs text-[var(--color-text)]">{{ b.tag || 'NULL' }}</td>
                <td class="table-td whitespace-nowrap font-mono text-xs text-[var(--color-text)]">{{ b.code || 'NULL' }}</td>
                <td class="table-td whitespace-nowrap font-mono text-xs text-[var(--color-text)]">{{ b.region || 'NULL' }}</td>
                <td class="table-td whitespace-nowrap font-mono text-xs text-[var(--color-text)]">{{ b.devtag || 'NULL' }}</td>
                <td class="table-td whitespace-nowrap font-mono text-xs text-[var(--color-text)]">{{ b.branchcode || 'NULL' }}</td>
                <td class="table-td whitespace-nowrap text-xs">
                  <span v-if="b.branchVisibility === 0" class="badge border border-[var(--color-border)] text-[var(--color-text-tertiary)]">隐藏</span>
                  <span v-if="b.branchEp === 1" class="badge border border-[var(--color-border)] text-[var(--color-warn)]">政企</span>
                  <span v-if="b.branchVisibility !== 0 && b.branchEp !== 1" class="text-[var(--color-text-tertiary)]">正常</span>
                </td>
                <td class="table-td text-right">
                  <button
                    type="button"
                    class="btn-secondary !px-2.5 !py-1 text-xs opacity-0 transition-opacity group-hover:opacity-100"
                    @click.stop="openBranchEdit(b)"
                  >
                    编辑
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-if="detail.branches.length === 0" class="px-6 py-10 text-center text-sm text-[var(--color-text-tertiary)]">
          暂无分支数据
        </div>
      </section>
    </template>

    <!-- 编辑弹窗（基准 / 分支通用） -->
    <RecordModal
      :open="formModalOpen"
      table="devices"
      :columns="deviceColumns"
      :record="formRecord"
      :form-options="formOpts"
      :title="formTitle"
      @close="formModalOpen = false"
      @saved="onFormSaved"
    />

    <!-- 新增分支：选择分支弹窗 -->
    <Teleport to="body">
      <Transition
        enter-active-class="transition duration-200 ease-out"
        enter-from-class="opacity-0"
        enter-to-class="opacity-100"
        leave-active-class="transition duration-150 ease-in"
        leave-from-class="opacity-100"
        leave-to-class="opacity-0"
      >
        <div v-if="pickerOpen" class="fixed inset-0 z-[90] overflow-y-auto" role="dialog" aria-modal="true" aria-label="选择要新增的分支">
          <div class="absolute inset-0 bg-black/40 backdrop-blur-sm" @click="pickerOpen = false"></div>
          <div class="relative mx-auto my-8 w-full max-w-3xl rounded-xl border border-[var(--color-border)] bg-[var(--color-bg-surface)] p-6 shadow-xl">
            <div class="mb-4 flex items-start justify-between gap-4">
              <div>
                <h3 class="text-base font-semibold text-[var(--color-text)]">新增分支</h3>
                <p class="mt-0.5 text-xs text-[var(--color-text-secondary)]">
                  选择一个尚未收录的分支，自动从基准复制信息后写入 devices 表
                </p>
              </div>
              <button
                type="button"
                class="flex h-8 w-8 shrink-0 items-center justify-center rounded-md text-[var(--color-text-tertiary)] transition-colors hover:bg-[var(--color-bg-subtle)] hover:text-[var(--color-text)]"
                aria-label="关闭"
                @click="pickerOpen = false"
              >
                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" aria-hidden="true">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M6 18 18 6M6 6l12 12" />
                </svg>
              </button>
            </div>

            <!-- 通过文件名识别分支 -->
            <div class="mb-4 rounded-lg border border-[var(--color-border)] bg-[var(--color-bg-subtle)] p-3">
              <label class="form-label !mb-1.5">
                通过文件名识别 <span class="text-xs font-normal text-[var(--color-text-tertiary)]">粘贴 ROM 文件名后点击提取</span>
              </label>
              <div class="flex gap-2">
                <input
                  v-model="branchFileInput"
                  type="text"
                  class="input-base flex-1 font-mono text-sm !py-1.5"
                  placeholder="如 houji_global-ota_full-OS3.0.303.0.WNCMIXM-user-16.0-65f80983d2.zip"
                />
                <button
                  type="button"
                  class="btn-primary shrink-0 !px-4 !py-1.5 text-xs"
                  :disabled="!branchFileInput.trim()"
                  @click="onBranchExtract"
                >
                  提取
                </button>
              </div>
              <p v-if="branchDetected" class="mt-1.5 text-xs text-green-600 dark:text-green-400">
                已识别：tag={{ branchDetected.tag }} · region={{ branchDetected.region }}
                <span v-if="branchDetected.carrier"> · carrier={{ branchDetected.carrier }}</span>
                <button
                  type="button"
                  class="ml-3 underline transition-colors hover:text-green-700 dark:hover:text-green-300"
                  @click="onBranchFilenameDetected"
                >
                  新增此分支 →
                </button>
              </p>
              <p v-else-if="branchExtracted && branchFileInput" class="mt-1.5 text-xs text-[var(--color-text-tertiary)]">
                无法识别该文件名，请检查格式
              </p>
            </div>

            <!-- 搜索 -->
            <div class="relative mb-4">
              <input
                v-model="pickerSearch"
                type="search"
                placeholder="搜索分支名称或 tag…"
                class="input-base !py-2 text-sm"
              />
            </div>

            <!-- 已收录同 tag 分支提示 -->
            <p v-if="branchDetected && matchedBranchCount > 0" class="mb-2 text-xs text-[var(--color-text-secondary)]">
              找到 <strong class="text-[var(--color-text)]">{{ matchedBranchCount }}</strong> 个同 tag（{{ branchDetected.tag }}）分支，已高亮标注
            </p>

            <!-- 列表 -->
            <ul class="max-h-80 space-y-1 overflow-y-auto">
              <li
                v-for="b in filteredAvailable"
                :key="b.id"
                class="cursor-pointer rounded-lg border px-4 py-3 transition-colors"
                :class="branchDetected && b.tag === branchDetected.tag
                  ? 'border-[var(--color-primary)]/40 bg-[var(--color-primary)]/5 hover:border-[var(--color-primary)]/60'
                  : 'border-transparent hover:border-[var(--color-border)] hover:bg-[var(--color-bg-subtle)]'"
                @click="onBranchPicked(b)"
              >
                <div class="flex items-center justify-between gap-3">
                  <div>
                    <span class="text-sm font-medium text-[var(--color-text)]">{{ b.nameZh || b.tag }}</span>
                    <span v-if="b.nameEn" class="ml-2 text-xs text-[var(--color-text-tertiary)]">{{ b.nameEn }}</span>
                  </div>
                  <span
                    class="badge shrink-0 border font-mono text-xs"
                    :class="branchDetected && b.tag === branchDetected.tag
                      ? 'border-[var(--color-primary)]/50 bg-[var(--color-primary)]/10 text-[var(--color-primary)]'
                      : 'border-[var(--color-border)] text-[var(--color-text-tertiary)]'"
                  >{{ b.tag }}</span>
                </div>
                <p class="mt-0.5 text-xs text-[var(--color-text-tertiary)]">
                  region={{ b.region || '—' }} · code={{ b.code || '无后缀' }}
                </p>
              </li>
            </ul>
            <div v-if="filteredAvailable.length === 0" class="px-4 py-8 text-center text-sm text-[var(--color-text-tertiary)]">
              没有可新增的分支（全部已收录）
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup>
const { getTableMeta, batchUpdateDevice, updateRecord, createRecord } = useAdminApi()
const { push } = useToast()

// ---- 机型列表 ----
const keyword = ref('')
const listOpen = ref(false)
const devices = ref([])
const selected = ref('')
const detail = ref(null)
const loading = ref(false)
const loadError = ref('')

// ---- 通用编辑弹窗 ----
const formModalOpen = ref(false)
const formRecord = ref(null)
const formTitle = ref('')
const deviceColumns = ref([])
const formOpts = ref({})

// ---- 新增分支选择器 ----
const pickerOpen = ref(false)
const pickerSearch = ref('')
const branchFileInput = ref('')
const branchDetected = ref(null)
const branchExtracted = ref(false)

// ---- 全局字段（device / devtag / full_brands / full_names / internal）----
const globalDevice = ref('')
const globalDevtag = ref('')
const globalInternal = ref('')
const globalFullBrands = ref('')
const globalFullNamesZh = ref('')
const globalFullNamesEn = ref('')
const globalSaving = ref(false)

const parseZhEn = (raw) => {
  if (!raw) return { zh: '', en: '' }
  try {
    const obj = JSON.parse(raw)
    if (obj && typeof obj === 'object' && !Array.isArray(obj)) {
      return { zh: obj.zh || '', en: obj.en || '' }
    }
  } catch { /* 忽略 */ }
  return { zh: raw, en: '' }
}

const globalDirty = computed(() => {
  if (!detail.value) return false
  const b = detail.value.baseline
  return globalDevice.value !== (b.device || '')
    || globalDevtag.value !== (b.devtag || '')
    || globalInternal.value !== (b.internal || '')
    || globalFullBrands.value !== (b.full_brands || '')
    || globalFullNamesZh.value !== (parseZhEn(b.full_names).zh)
    || globalFullNamesEn.value !== (parseZhEn(b.full_names).en)
})

const filtered = computed(() => {
  const k = keyword.value.trim().toLowerCase()
  const all = k
    ? devices.value.filter(
        (d) => d.device.toLowerCase().includes(k) || (d.name && d.name.toLowerCase().includes(k)),
      )
    : devices.value
  return all.slice(0, 50)
})

const filteredAvailable = computed(() => {
  if (!detail.value?.availableBranches) return []
  const k = pickerSearch.value.trim().toLowerCase()
  const all = detail.value.availableBranches
  if (!k) return all
  return all.filter(
    (b) =>
      (b.nameZh && b.nameZh.toLowerCase().includes(k))
      || (b.nameEn && b.nameEn.toLowerCase().includes(k))
      || (b.tag && b.tag.toLowerCase().includes(k))
      || (b.region && b.region.toLowerCase().includes(k)),
  )
})

/** 文件名提取后，找到的同 tag 分支数量 */
const matchedBranchCount = computed(() => {
  if (!branchDetected.value?.tag || !detail.value?.availableBranches) return 0
  return detail.value.availableBranches.filter((b) => b.tag === branchDetected.value.tag).length
})

const onBlur = () => {
  setTimeout(() => { listOpen.value = false }, 120)
}

const reloadDevices = async () => {
  const res = await $fetch('/api/devices-manager')
  devices.value = res.devices
}

const select = (d) => {
  selected.value = d.device
  keyword.value = ''
  listOpen.value = false
  loadDetail(d.device)
}

const clear = () => {
  selected.value = ''
  detail.value = null
}

const loadDetail = async (device) => {
  loading.value = true
  loadError.value = ''
  try {
    detail.value = await $fetch('/api/devices-manager', { params: { device } })
    // 初始化全局字段
    const b = detail.value.baseline
    globalDevice.value = b.device || ''
    globalDevtag.value = b.devtag || ''
    globalInternal.value = b.internal || ''
    globalFullBrands.value = b.full_brands || ''
    const { zh, en } = parseZhEn(b.full_names)
    globalFullNamesZh.value = zh
    globalFullNamesEn.value = en
  } catch (e) {
    loadError.value = errorMessage(e)
  } finally {
    loading.value = false
  }
}

/** 确保 deviceColumns 已加载 */
const ensureColumns = async () => {
  if (deviceColumns.value.length === 0) {
    deviceColumns.value = await getTableMeta('devices').then((r) => r.columns)
  }
}

/** 根据 branches 表构建下拉选项（去重 tags / regions） */
const buildFormOptions = () => {
  if (!detail.value?.allBranches) return {}
  const all = detail.value.allBranches
  const tags = [...new Set(all.map((b) => b.tag).filter(Boolean))].sort()
  const regions = [...new Set(all.map((b) => b.region).filter(Boolean))].sort()
  // 存储值用双引号包裹（如 "Xiaomi, Redmi"），label 展示不带引号
  const brandsOptions = ['Xiaomi', 'Redmi', 'POCO', 'Xiaomi, Redmi', 'Xiaomi, POCO', 'Redmi, POCO', 'Xiaomi, Redmi, POCO'].map((v) => ({ value: `"${v}"`, label: v }))
  const carrierOptions = [
    { value: '', label: '(无运营商)' },
    { value: 'chinatelecom', label: '中国电信' },
    { value: 'chinamobile', label: '中国移动' },
    { value: 'chinaunicom', label: '中国联通' },
    { value: 'chinatelecom, chinamobile, chinaunicom', label: '全运营商' },
  ]
  return {
    tag: tags.map((t) => ({ value: t, label: t })),
    region: regions.map((r) => ({ value: r, label: r })),
    brands: brandsOptions,
    carrier: carrierOptions,
  }
}

// ---- 分支编辑 ----
const openBranchEdit = async (b) => {
  await ensureColumns()
  formRecord.value = {
    id: b.id,
    device: b.device,
    code: b.code,
    tag: b.tag,
    region: b.region,
    devtag: b.devtag,
    devcode: b.devcode,
    branchcode: b.branchcode,
    carrier: b.carrier || '',
    full_brands: b.full_brands || '',
    brands: b.brands || '',
    full_names: b.full_names || '',
    names: b.names || '',
    xiaomi: b.xiaomi || '',
    redmi: b.redmi || '',
    poco: b.poco || '',
    image: b.image || '',
    launch_date: b.launch_date || '',
    internal: b.internal || '',
    model: b.model || '',
  }
  formOpts.value = buildFormOptions()
  formTitle.value = `编辑分支 #${b.id} · ${b.code}`
  formModalOpen.value = true
}

const onFormSaved = () => {
  if (selected.value) loadDetail(selected.value)
}

// ---- 全局字段生成与保存 ----
/** 从全部分支的品牌字段自动生成 full_brands（固定顺序：Xiaomi → Redmi → POCO） */
const genGlobalFullBrands = () => {
  if (!detail.value?.allBranchesBrandData) return
  const brandFields = [
    { field: 'xiaomi', label: 'Xiaomi' },
    { field: 'redmi', label: 'Redmi' },
    { field: 'poco', label: 'POCO' },
  ]
  const detected = new Set()
  for (const branch of detail.value.allBranchesBrandData) {
    for (const { field, label } of brandFields) {
      const raw = branch[field]
      if (!raw) continue
      try {
        const obj = JSON.parse(raw)
        if (obj && typeof obj === 'object' && !Array.isArray(obj) && Object.keys(obj).length > 0) {
          detected.add(label)
        }
      } catch { /* 忽略 */ }
    }
  }
  if (detected.size === 0) {
    push('info', '全部分支的品牌字段均为空，无法生成')
    return
  }
  // 按固定顺序输出：Xiaomi → Redmi → POCO
  globalFullBrands.value = JSON.stringify(brandFields.map((b) => b.label).filter((l) => detected.has(l)))
  push('success', `full_brands 已从全部 ${detail.value.allBranchesBrandData.length} 个分支自动生成`)
}

/** 从全部分支的名称字段自动生成 full_names（多品牌用 / 拼接，顺序：Xiaomi → Redmi → POCO） */
const genGlobalFullNames = () => {
  if (!detail.value?.allBranchesBrandData) return
  const brandFields = [
    { field: 'xiaomi', label: 'Xiaomi' },
    { field: 'redmi', label: 'Redmi' },
    { field: 'poco', label: 'POCO' },
  ]
  const namesZh = []
  const namesEn = []
  const seenZh = new Set()
  const seenEn = new Set()
  for (const { field } of brandFields) {
    for (const branch of detail.value.allBranchesBrandData) {
      const raw = branch[field]
      if (!raw) continue
      try {
        const obj = JSON.parse(raw)
        if (obj && typeof obj === 'object' && !Array.isArray(obj)) {
          if (obj.zh && !seenZh.has(obj.zh)) { seenZh.add(obj.zh); namesZh.push(obj.zh) }
          if (obj.en && !seenEn.has(obj.en)) { seenEn.add(obj.en); namesEn.push(obj.en) }
        }
      } catch { /* 忽略 */ }
    }
  }
  if (namesZh.length === 0 && namesEn.length === 0) {
    push('info', '全部分支的名称字段均为空，无法生成')
    return
  }
  globalFullNamesZh.value = namesZh.join(' / ')
  globalFullNamesEn.value = namesEn.join(' / ')
  push('success', `full_names 已从全部 ${detail.value.allBranchesBrandData.length} 个分支自动生成`)
}

/** 保存全局字段并同步至所有分支 */
const saveGlobalFields = async () => {
  if (!detail.value) return
  globalSaving.value = true
  try {
    const oldDevice = detail.value.baseline.device
    const newDevice = globalDevice.value.trim()
    if (!newDevice) {
      push('error', 'device 不能为空')
      globalSaving.value = false
      return
    }
    const fields = {}
    // device
    fields.device = newDevice
    // devtag
    fields.devtag = globalDevtag.value.trim() || null
    // internal
    fields.internal = globalInternal.value.trim() || null
    // full_brands
    if (globalFullBrands.value) {
      try { JSON.parse(globalFullBrands.value) } catch {
        push('error', 'full_brands 不是合法的 JSON 数组')
        globalSaving.value = false
        return
      }
      fields.full_brands = globalFullBrands.value
    } else {
      fields.full_brands = ''
    }
    // full_names
    const nameObj = {}
    if (globalFullNamesZh.value) nameObj.zh = globalFullNamesZh.value
    if (globalFullNamesEn.value) nameObj.en = globalFullNamesEn.value
    fields.full_names = Object.keys(nameObj).length > 0 ? JSON.stringify(nameObj) : ''

    // 同步至所有同机型分支（使用旧 device 名称定位）
    const res = await batchUpdateDevice('devices', oldDevice, fields)
    push('success', `全局字段已保存并同步至 ${res.affectedRows} 条记录`)
    // 刷新（使用新 device 名称）
    loadDetail(newDevice)
    selected.value = newDevice
  } catch (e) {
    push('error', errorMessage(e))
  } finally {
    globalSaving.value = false
  }
}

// ---- 新增机型 ----
const createOpen = ref(false)
const createDeviceName = ref('')
const createDevtag = ref('')
const createDevcode = ref('')
const createFilename = ref('')
const createDetected = ref(null)
const creating = ref(false)

const openCreateDevice = () => {
  createDeviceName.value = ''
  createDevtag.value = ''
  createDevcode.value = ''
  createFilename.value = ''
  createDetected.value = null
  createOpen.value = true
}

/** 用户在新增机型弹窗点击「提取」时，从文件名中识别并填充设备信息 */
const onCreateExtract = () => {
  const info = parseRomFilename(createFilename.value)
  createDetected.value = info
  if (info) {
    createDeviceName.value = info.device
    createDevcode.value = info.devcode || ''
    push('success', `已从文件名提取设备信息：${info.device}`)
  } else {
    push('error', '无法识别该文件名，请检查格式')
  }
}

const doCreateDevice = async () => {
  const name = createDeviceName.value.trim()
  if (!name) return
  creating.value = true
  try {
    await ensureColumns()
    const detected = createDetected.value
    const payload = {
      device: name,
      code: detected?.code ? `${name}${detected.code}` : name,
      tag: detected?.tag || 'CnOO',
      region: detected?.region || 'cn',
      devtag: createDevtag.value.trim() || null,
      devcode: createDevcode.value.trim() || null,
      branchcode: genBranchcode(name, detected?.code || ''),
      carrier: detected?.carrier
        ? JSON.stringify(['', detected.carrier])
        : "['','chinatelecom','chinamobile','chinaunicom']",
    }
    const res = await createRecord('devices', payload)
    push('success', `机型 ${name} 已创建（id=${res.id}）`)
    createOpen.value = false
    // 选中新机型并加载详情
    selected.value = name
    await loadDetail(name)
  } catch (e) {
    push('error', errorMessage(e))
  } finally {
    creating.value = false
  }
}

// ---- 新增分支 ----
const openBranchPicker = () => {
  pickerSearch.value = ''
  branchFileInput.value = ''
  branchDetected.value = null
  branchExtracted.value = false
  pickerOpen.value = true
}

/** 用户在新增分支弹窗点击「提取」时，从文件名中识别分支 */
const onBranchExtract = () => {
  branchExtracted.value = true
  branchDetected.value = parseRomFilename(branchFileInput.value)
  if (branchDetected.value) {
    push('success', `已识别分支：tag=${branchDetected.value.tag} region=${branchDetected.value.region}`)
  }
}

/** 通过文件名识别到分支后，直接打开编辑弹窗新增该分支 */
const onBranchFilenameDetected = async () => {
  const info = branchDetected.value
  if (!info || !detail.value?.baseline) return
  pickerOpen.value = false
  await ensureColumns()
  const b = detail.value.baseline
  const suffix = info.code || ''
  const newCode = suffix ? `${b.device}${suffix}` : b.device

  // 生成 carrier 列表
  let carrierValue = b.carrier || ''
  if (info.carrier) {
    carrierValue = JSON.stringify(['', info.carrier])
  }

  formRecord.value = {
    // id 留空 = 新增
    device: b.device,
    code: newCode,
    tag: info.tag || '',
    region: info.region || '',
    devtag: b.devtag || '',
    devcode: info.devcode || b.devcode || '',
    branchcode: genBranchcode(b.device, suffix),
    carrier: carrierValue,
    brands: wrapBrands(b.brands),
    full_names: '',
    names: '',
    xiaomi: '',
    redmi: '',
    poco: '',
    image: b.image || '',
    launch_date: b.launch_date || '',
    internal: b.internal || '',
    model: b.model || '',
  }
  formOpts.value = buildFormOptions()
  formTitle.value = `新增分支 · tag=${info.tag} region=${info.region}（${newCode}）`
  formModalOpen.value = true
}

/** 品牌列表用双引号包裹：Redmi → "Redmi"，Xiaomi, Redmi → "Xiaomi, Redmi"；已包裹则保持不变 */
const wrapBrands = (raw) => {
  const v = (raw || '').trim()
  if (!v) return ''
  if (v.startsWith('"') && v.endsWith('"')) return v
  return `"${v}"`
}

/** 根据 branches.code 后缀生成 branchcode（大写设备名 + 后缀首字母大写） */
function genBranchcode(deviceName, suffix) {
  const dev = deviceName.toUpperCase()
  if (!suffix) return dev
  // _global → Global, _dc_global → DCGlobal, _tw_global → TWGlobal
  const cleaned = suffix.replace(/^_/, '')
  const parts = cleaned.split('_')
  const sfx = parts.map((p) => p.charAt(0).toUpperCase() + p.slice(1).toLowerCase()).join('')
  return dev + sfx
}

const onBranchPicked = async (branch) => {
  pickerOpen.value = false
  await ensureColumns()
  const b = detail.value.baseline
  const suffix = branch.code || ''
  const newCode = suffix ? `${b.device}${suffix}` : b.device

  formRecord.value = {
    // id 留空 = 新增
    device: b.device,
    code: newCode,
    tag: branch.tag || '',
    region: branch.region || '',
    devtag: b.devtag || '',
    devcode: b.devcode || '',
    branchcode: genBranchcode(b.device, suffix),
    carrier: branch.carrier || b.carrier || '',
    brands: wrapBrands(b.brands),
    full_names: '',
    names: '',
    xiaomi: '',
    redmi: '',
    poco: '',
    image: b.image || '',
    launch_date: b.launch_date || '',
    internal: b.internal || '',
    model: b.model || '',
  }
  formOpts.value = buildFormOptions()
  formTitle.value = `新增分支 · ${branch.nameZh || branch.tag}（${newCode}）`
  formModalOpen.value = true
}

// ---- ROM 文件名解析 ----
/** 从 ROM 文件名中提取设备信息（设备名、tag、region、devcode、carrier 等） */
function parseRomFilename(filename) {
  const name = filename.trim()
  if (!name) return null

  let deviceName = ''
  let codeSuffix = ''
  let version = ''

  if (name.endsWith('.zip') && name.startsWith('miui_')) {
    // 旧 MIUI 格式: miui_DEVICE_{version}_{android}.zip
    const parts = name.replace('.zip', '').split('_')
    if (parts.length >= 3) {
      deviceName = parts[1].toLowerCase()
      version = parts[2]
    }
  } else if (name.endsWith('.zip') && name.includes('-ota_full-')) {
    // 新 HyperOS 格式: {device}-ota_full-{version}-user-{android}-{hash}.zip
    const beforeOta = name.split('-ota_full-')[0]
    const afterOta = name.split('-ota_full-')[1]
    version = afterOta ? afterOta.split('-')[0] : ''
    if (beforeOta.includes('_')) {
      const idx = beforeOta.indexOf('_')
      deviceName = beforeOta.substring(0, idx).toLowerCase()
      codeSuffix = '_' + beforeOta.substring(idx + 1)
    } else {
      deviceName = beforeOta.toLowerCase()
    }
  } else if (name.endsWith('.tgz')) {
    // Fastboot 格式: {device}[_suffix]_images_{version}_{date}_{android}_{region}[_{carrier}]_{hash}.tgz
    const base = name.replace('.tgz', '')
    const imagesIdx = base.indexOf('_images_')
    if (imagesIdx === -1) return null
    const prefix = base.substring(0, imagesIdx)
    version = base.split('_images_')[1].split('_')[0]
    if (prefix.includes('_')) {
      const idx = prefix.indexOf('_')
      deviceName = prefix.substring(0, idx).toLowerCase()
      codeSuffix = '_' + prefix.substring(idx + 1)
    } else {
      deviceName = prefix.toLowerCase()
    }
  } else {
    return null
  }

  if (!deviceName || !version) return null

  // tag -> region 映射表
  const TAG_MAP = {
    CNXM: { region: 'cn', carrier: '' },
    MIXM: { region: 'global', carrier: '' },
    MIDC: { region: 'global', carrier: '' },
    MIHG: { region: 'global', carrier: '' },
    INXM: { region: 'in', carrier: '' },
    IDXM: { region: 'id', carrier: '' },
    TWXM: { region: 'tw', carrier: '' },
    RUXM: { region: 'ru', carrier: '' },
    EUXM: { region: 'eea', carrier: '' },
    EUTF: { region: 'eea', carrier: 'telefonica' },
    EUOR: { region: 'eea', carrier: 'orange' },
    EUVF: { region: 'eea', carrier: 'vf' },
    EUHG: { region: 'eea', carrier: 'threehk' },
    TRXM: { region: 'tr', carrier: '' },
    JPXM: { region: 'jp', carrier: '' },
    KRXM: { region: 'kr', carrier: '' },
    LMCR: { region: 'lm', carrier: '' },
    MXTC: { region: 'mx', carrier: '' },
    ZAMT: { region: 'za', carrier: '' },
    ZAVC: { region: 'za', carrier: '' },
    INFK: { region: 'in', carrier: '' },
  }

  let tag = ''
  let region = ''
  let carrier = ''
  let devcode = ''

  if (version.toUpperCase().includes('CNXM')) {
    region = 'cn'
    tag = 'CnOO'
    const codeMatch = version.match(/([A-Z0-9]{6,})$/i)
    if (codeMatch) devcode = codeMatch[1]
  } else {
    // 从后缀代码提取 tag
    const codeMatch = version.match(/([A-Z0-9]{4,})$/i)
    if (codeMatch) {
      const code = codeMatch[1]
      const tagInfo = TAG_MAP[code]
      if (tagInfo) {
        tag = code
        region = tagInfo.region
        carrier = tagInfo.carrier
      } else if (code.length >= 6) {
        // 6+ 位：前4位=tag，后2位=运营商
        const baseTag = code.substring(0, 4)
        const carrierCode = code.substring(4)
        const baseInfo = TAG_MAP[baseTag]
        if (baseInfo) {
          tag = baseTag
          region = baseInfo.region
          carrier = carrierCode
        } else {
          tag = code
          region = code.toLowerCase()
        }
      } else {
        tag = code
        region = code.toLowerCase()
      }
    }
    devcode = codeMatch ? codeMatch[1] : ''
  }

  return {
    filename: name,
    device: deviceName,
    tag,
    region,
    code: codeSuffix,
    carrier,
    devcode,
    version,
  }
}

onMounted(async () => {
  try {
    await reloadDevices()
  } catch (e) {
    push('error', errorMessage(e))
  }
})
</script>
