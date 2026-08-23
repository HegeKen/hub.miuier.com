<template>
  <ClientOnly>
    <div id="top">
      <Header />
      <main id="main-content" tabindex="-1">
        <NuxtPage />
      </main>
      <Footer />
      <MiRoms />
    </div>
  </ClientOnly>
</template>
