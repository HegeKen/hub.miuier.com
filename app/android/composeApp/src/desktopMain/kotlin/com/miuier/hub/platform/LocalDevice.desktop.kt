package com.miuier.hub.platform

/**
 * 桌面端只为本地预览 / 无头截图，没有「本机机型」这个概念 —— 返回 null，
 * 「本机信息」tab 就不会出现在桌面预览里（截图脚本可以显式注入一个假的来看效果）。
 */
actual fun currentLocalDevice(): LocalDevice? = null
