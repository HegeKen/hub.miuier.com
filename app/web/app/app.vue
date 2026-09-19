<template>
  <!-- MiRoms 负责 <html lang> / <html dir>，必须留在 ClientOnly 之外：
       放进 ClientOnly 只在浏览器端执行，SSR 的 HTML 就没有 lang / dir，
       阿拉伯语等 RTL 语言会先按 LTR 排版再翻转成 RTL，出现可见的方向闪烁。 -->
  <MiRoms />
  <ClientOnly>
    <div id="top">
      <Header />
      <main id="main-content" tabindex="-1">
        <NuxtPage />
      </main>
      <Footer />
    </div>
  </ClientOnly>
</template>
