<template>
  <TableManager
    table="xms_app_release"
    title="XMS 应用发布物"
    description="xms_app_release：同一个应用版本（pkg + pkg_ver）只存一行元数据（多语言名称 names、安装包信息、更新日志），被多次推送共享。按推送整理请用「XMS 应用更新」页；推送 ↔ 发布物的关联在 xms_app 表"
  />
</template>
