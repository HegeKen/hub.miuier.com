#!/usr/bin/env python3
"""生成 Android 端的多语言资源 `ui/i18n/Locales.kt`。

用法：
    python3 scripts/make_locales.py

语言列表与词条来源都对齐网页端 `app/web/i18n/locales/*.ts`（18 种语言）：

- 能对上的 UI 词条（下载 / 更新日志 / 品牌 / 支持系统 / 关于 / 免责声明 / 作者主页 …）
  以及**全部区域名与运营商名**直接从网页端语言包取，不重复翻译；
- 只在本 App 出现的词条（高速下载、卡刷包/线刷包有无、外观、技术栈 …）
  在下面的 `APP_STRINGS` 里维护。

网页端加了新语言时，这里跑一遍即可同步。
"""

import json
import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
WEB_LOCALES = HERE / '..' / '..' / 'web' / 'i18n' / 'locales'
OUT = HERE / '..' / 'composeApp' / 'src' / 'commonMain' / 'kotlin' / 'com' / 'miuier' / 'hub' / 'ui' / 'i18n' / 'Locales.kt'

# 顺序与网页端 nuxt.config.ts 的 i18n.locales 一致（隐藏别名 zh / en 不参与）
LANGS = [
    ('ZH_HANS', 'zh-cn', '简体中文'),
    ('ZH_HANT', 'zh-tw', '繁體中文'),
    ('EN', 'en-us', 'English'),
    ('JA', 'ja', '日本語'),
    ('KO', 'ko', '한국어'),
    ('RU', 'ru', 'Русский'),
    ('UK', 'uk', 'Українська'),
    ('PL', 'pl', 'Polski'),
    ('DE', 'de', 'Deutsch'),
    ('FR', 'fr', 'Français'),
    ('IT', 'it', 'Italiano'),
    ('ES', 'es', 'Español'),
    ('PT', 'pt', 'Português'),
    ('TR', 'tr', 'Türkçe'),
    ('ID', 'id', 'Bahasa Indonesia'),
    ('VI', 'vi', 'Tiếng Việt'),
    ('TH', 'th', 'ไทย'),
    ('AR', 'ar', 'العربية'),
]

# 直接复用网页端同名字段的词条：本地 key -> 网页端 key
REUSE = {
    'home': 'home',
    'devices': 'devices',
    'roms': 'roms',
    'download': 'download',
    'recovery': 'recovery',
    'fastboot': 'fastboot',
    'changelog': 'changelog',
    'noChangelog': 'noLogs',
    'deviceCode': 'devcode',
    'brand': 'brand',
    'supports': 'supports',
    'enterprise': 'ep',
    'stable': 'stable',
    'dev': 'dev',
    'about': 'about',
    'authorSite': 'authorsite',
    'feedback': 'feedback',
    'searchDevice': 'searchPlaceholder',
}

# 只在本 App 出现的词条。占位符统一写成 {name}，渲染时用 Strings.fill 替换。
APP_STRINGS = {
    'settings': {
        'zh-cn': '设置', 'zh-tw': '設定', 'en-us': 'Settings', 'ja': '設定', 'ko': '설정',
        'ru': 'Настройки', 'uk': 'Налаштування', 'pl': 'Ustawienia', 'de': 'Einstellungen',
        'fr': 'Paramètres', 'it': 'Impostazioni', 'es': 'Ajustes', 'pt': 'Configurações',
        'tr': 'Ayarlar', 'id': 'Pengaturan', 'vi': 'Cài đặt', 'th': 'การตั้งค่า', 'ar': 'الإعدادات',
    },
    # 下拉刷新：MIUIX 的 PullToRefresh 要一个 4 元文案列表（下拉 / 松开 / 刷新中 / 完成）
    'refreshPull': {
        'zh-cn': '下拉刷新', 'zh-tw': '下拉重新整理', 'en-us': 'Pull down to refresh',
        'ja': '下に引いて更新', 'ko': '아래로 당겨 새로 고침', 'ru': 'Потяните вниз',
        'uk': 'Потягніть вниз', 'pl': 'Pociągnij w dół', 'de': 'Zum Aktualisieren ziehen',
        'fr': 'Tirez vers le bas', 'it': 'Tira verso il basso', 'es': 'Desliza hacia abajo',
        'pt': 'Puxe para baixo', 'tr': 'Aşağı çekin', 'id': 'Tarik ke bawah',
        'vi': 'Kéo xuống', 'th': 'ดึงลงเพื่อรีเฟรช', 'ar': 'اسحب للأسفل',
    },
    'refreshRelease': {
        'zh-cn': '松开刷新', 'zh-tw': '放開重新整理', 'en-us': 'Release to refresh',
        'ja': '離して更新', 'ko': '놓아서 새로 고침', 'ru': 'Отпустите для обновления',
        'uk': 'Відпустіть для оновлення', 'pl': 'Puść, aby odświeżyć',
        'de': 'Zum Aktualisieren loslassen', 'fr': 'Relâchez pour actualiser',
        'it': 'Rilascia per aggiornare', 'es': 'Suelta para actualizar',
        'pt': 'Solte para atualizar', 'tr': 'Yenilemek için bırakın',
        'id': 'Lepaskan untuk menyegarkan', 'vi': 'Thả để làm mới',
        'th': 'ปล่อยเพื่อรีเฟรช', 'ar': 'اترك للتحديث',
    },
    'refreshRefreshing': {
        'zh-cn': '正在刷新…', 'zh-tw': '正在重新整理…', 'en-us': 'Refreshing…',
        'ja': '更新中…', 'ko': '새로 고치는 중…', 'ru': 'Обновление…',
        'uk': 'Оновлення…', 'pl': 'Odświeżanie…', 'de': 'Wird aktualisiert…',
        'fr': 'Actualisation…', 'it': 'Aggiornamento…', 'es': 'Actualizando…',
        'pt': 'Atualizando…', 'tr': 'Yenileniyor…', 'id': 'Menyegarkan…',
        'vi': 'Đang làm mới…', 'th': 'กำลังรีเฟรช…', 'ar': 'جارٍ التحديث…',
    },
    'refreshDone': {
        'zh-cn': '刷新完成', 'zh-tw': '重新整理完成', 'en-us': 'Refreshed',
        'ja': '更新しました', 'ko': '새로 고침 완료', 'ru': 'Обновлено',
        'uk': 'Оновлено', 'pl': 'Odświeżono', 'de': 'Aktualisiert',
        'fr': 'Actualisé', 'it': 'Aggiornato', 'es': 'Actualizado',
        'pt': 'Atualizado', 'tr': 'Yenilendi', 'id': 'Segar',
        'vi': 'Đã làm mới', 'th': 'รีเฟรชแล้ว', 'ar': 'تم التحديث',
    },
    # 设置页「项目」分组的标题（仓库 / 意见反馈 / 作者主页三行）
    'project': {
        'zh-cn': '项目', 'zh-tw': '專案', 'en-us': 'Project', 'ja': 'プロジェクト',
        'ko': '프로젝트', 'ru': 'Проект', 'uk': 'Проєкт', 'pl': 'Projekt',
        'de': 'Projekt', 'fr': 'Projet', 'it': 'Progetto', 'es': 'Proyecto',
        'pt': 'Projeto', 'tr': 'Proje', 'id': 'Proyek', 'vi': 'Dự án',
        'th': 'โครงการ', 'ar': 'المشروع',
    },
    'back': {
        'zh-cn': '返回', 'zh-tw': '返回', 'en-us': 'Back', 'ja': '戻る', 'ko': '뒤로',
        'ru': 'Назад', 'uk': 'Назад', 'pl': 'Wstecz', 'de': 'Zurück', 'fr': 'Retour',
        'it': 'Indietro', 'es': 'Atrás', 'pt': 'Voltar', 'tr': 'Geri', 'id': 'Kembali',
        'vi': 'Quay lại', 'th': 'ย้อนกลับ', 'ar': 'رجوع',
    },
    # —— 本机信息：只在本机机型出现在 hub 名单里时，底栏才会多出这一项 ——
    'myDevice': {
        'zh-cn': '本机信息', 'zh-tw': '本機資訊', 'en-us': 'My device', 'ja': 'この端末',
        'ko': '내 기기', 'ru': 'Моё устройство', 'uk': 'Мій пристрій',
        'pl': 'Moje urządzenie', 'de': 'Mein Gerät', 'fr': 'Mon appareil',
        'it': 'Il mio dispositivo', 'es': 'Mi dispositivo', 'pt': 'Meu dispositivo',
        'tr': 'Cihazım', 'id': 'Perangkat saya', 'vi': 'Thiết bị của tôi',
        'th': 'อุปกรณ์ของฉัน', 'ar': 'جهازي',
    },
    'thisDevice': {
        'zh-cn': '本机', 'zh-tw': '本機', 'en-us': 'This device', 'ja': 'この端末',
        'ko': '이 기기', 'ru': 'Это устройство', 'uk': 'Цей пристрій',
        'pl': 'To urządzenie', 'de': 'Dieses Gerät', 'fr': 'Cet appareil',
        'it': 'Questo dispositivo', 'es': 'Este dispositivo', 'pt': 'Este dispositivo',
        'tr': 'Bu cihaz', 'id': 'Perangkat ini', 'vi': 'Thiết bị này',
        'th': 'อุปกรณ์นี้', 'ar': 'هذا الجهاز',
    },
    'systemVersion': {
        'zh-cn': '系统版本', 'zh-tw': '系統版本', 'en-us': 'System version',
        'ja': 'システムバージョン', 'ko': '시스템 버전', 'ru': 'Версия системы',
        'uk': 'Версія системи', 'pl': 'Wersja systemu', 'de': 'Systemversion',
        'fr': 'Version du système', 'it': 'Versione sistema', 'es': 'Versión del sistema',
        'pt': 'Versão do sistema', 'tr': 'Sistem sürümü', 'id': 'Versi sistem',
        'vi': 'Phiên bản hệ thống', 'th': 'เวอร์ชันระบบ', 'ar': 'إصدار النظام',
    },
    'androidVersion': {
        'zh-cn': 'Android 版本', 'zh-tw': 'Android 版本', 'en-us': 'Android version',
        'ja': 'Android バージョン', 'ko': 'Android 버전', 'ru': 'Версия Android',
        'uk': 'Версія Android', 'pl': 'Wersja Androida', 'de': 'Android-Version',
        'fr': 'Version Android', 'it': 'Versione Android', 'es': 'Versión de Android',
        'pt': 'Versão do Android', 'tr': 'Android sürümü', 'id': 'Versi Android',
        'vi': 'Phiên bản Android', 'th': 'เวอร์ชัน Android', 'ar': 'إصدار Android',
    },
    'runtimeVersion': {
        'zh-cn': '运行时版本', 'zh-tw': '執行階段版本', 'en-us': 'Runtime version',
        'ja': 'ランタイム版', 'ko': '런타임 버전', 'ru': 'Версия среды',
        'uk': 'Версія середовища', 'pl': 'Wersja środowiska', 'de': 'Laufzeitversion',
        'fr': "Version d'exécution", 'it': 'Versione runtime', 'es': 'Versión de ejecución',
        'pt': 'Versão de execução', 'tr': 'Çalışma zamanı', 'id': 'Versi runtime',
        'vi': 'Phiên bản runtime', 'th': 'เวอร์ชันรันไทม์', 'ar': 'إصدار بيئة التشغيل',
    },
    'myDeviceFound': {
        'zh-cn': '已识别到本机机型', 'zh-tw': '已識別到本機機型', 'en-us': 'Device recognised',
        'ja': '端末を識別しました', 'ko': '기기를 인식했습니다', 'ru': 'Устройство распознано',
        'uk': 'Пристрій розпізнано', 'pl': 'Rozpoznano urządzenie', 'de': 'Gerät erkannt',
        'fr': 'Appareil reconnu', 'it': 'Dispositivo riconosciuto', 'es': 'Dispositivo reconocido',
        'pt': 'Dispositivo reconhecido', 'tr': 'Cihaz tanındı', 'id': 'Perangkat dikenali',
        'vi': 'Đã nhận diện thiết bị', 'th': 'รู้จักอุปกรณ์แล้ว', 'ar': 'تم التعرّف على الجهاز',
    },
    'myDeviceSummary': {
        'zh-cn': 'hub 已收录 {count} 个刷机包', 'zh-tw': 'hub 已收錄 {count} 個刷機包',
        'en-us': 'hub has {count} ROMs for it', 'ja': 'hub に {count} 件の ROM があります',
        'ko': 'hub에 ROM {count}개가 있습니다', 'ru': 'В hub {count} прошивок',
        'uk': 'У hub {count} прошивок', 'pl': 'hub ma {count} ROM-ów',
        'de': 'hub hat {count} ROMs', 'fr': 'hub contient {count} ROM',
        'it': 'hub ha {count} ROM', 'es': 'hub tiene {count} ROM',
        'pt': 'O hub tem {count} ROMs', 'tr': "hub'da {count} ROM var",
        'id': 'hub memiliki {count} ROM', 'vi': 'hub có {count} ROM',
        'th': 'hub มี ROM {count} รายการ', 'ar': 'يحتوي hub على {count} حزمة',
    },
    'searchRom': {
        'zh-cn': '搜索机型 / 版本号', 'zh-tw': '搜尋機型 / 版本號', 'en-us': 'Search device or version',
        'ja': '機種 / バージョンで検索', 'ko': '기기 / 버전 검색', 'ru': 'Поиск устройства или версии',
        'uk': 'Пошук пристрою або версії', 'pl': 'Szukaj modelu lub wersji',
        'de': 'Gerät oder Version suchen', 'fr': "Rechercher un appareil ou une version",
        'it': 'Cerca dispositivo o versione', 'es': 'Buscar dispositivo o versión',
        'pt': 'Pesquisar dispositivo ou versão', 'tr': 'Cihaz veya sürüm ara',
        'id': 'Cari perangkat atau versi', 'vi': 'Tìm thiết bị hoặc phiên bản',
        'th': 'ค้นหารุ่นหรือเวอร์ชัน', 'ar': 'ابحث عن الجهاز أو الإصدار',
    },
    'all': {
        'zh-cn': '全部', 'zh-tw': '全部', 'en-us': 'All', 'ja': 'すべて', 'ko': '전체',
        'ru': 'Все', 'uk': 'Усі', 'pl': 'Wszystkie', 'de': 'Alle', 'fr': 'Tous',
        'it': 'Tutti', 'es': 'Todos', 'pt': 'Todos', 'tr': 'Tümü', 'id': 'Semua',
        'vi': 'Tất cả', 'th': 'ทั้งหมด', 'ar': 'الكل',
    },
    'deviceCount': {
        'zh-cn': '共 {count} 款机型', 'zh-tw': '共 {count} 款機型', 'en-us': '{count} devices',
        'ja': '{count} 機種', 'ko': '기기 {count}종', 'ru': 'Устройств: {count}',
        'uk': 'Пристроїв: {count}', 'pl': 'Modeli: {count}', 'de': '{count} Geräte',
        'fr': '{count} appareils', 'it': '{count} dispositivi', 'es': '{count} dispositivos',
        'pt': '{count} dispositivos', 'tr': '{count} cihaz', 'id': '{count} perangkat',
        'vi': '{count} thiết bị', 'th': '{count} รุ่น', 'ar': '{count} جهاز',
    },
    'entryCount': {
        'zh-cn': '共 {count} 条', 'zh-tw': '共 {count} 筆', 'en-us': '{count} entries',
        'ja': '{count} 件', 'ko': '{count}개', 'ru': 'Записей: {count}',
        'uk': 'Записів: {count}', 'pl': 'Wpisów: {count}', 'de': '{count} Einträge',
        'fr': '{count} entrées', 'it': '{count} voci', 'es': '{count} entradas',
        'pt': '{count} entradas', 'tr': '{count} kayıt', 'id': '{count} entri',
        'vi': '{count} mục', 'th': '{count} รายการ', 'ar': '{count} عنصر',
    },
    'romSummary': {
        'zh-cn': '{count} 个包 · {devices} 款机型', 'zh-tw': '{count} 個包 · {devices} 款機型',
        'en-us': '{count} ROMs · {devices} devices', 'ja': '{count} 個 · {devices} 機種',
        'ko': '{count}개 · 기기 {devices}종', 'ru': 'Пакетов: {count} · устройств: {devices}',
        'uk': 'Пакетів: {count} · пристроїв: {devices}', 'pl': 'Pakietów: {count} · modeli: {devices}',
        'de': '{count} Pakete · {devices} Geräte', 'fr': '{count} ROM · {devices} appareils',
        'it': '{count} ROM · {devices} dispositivi', 'es': '{count} ROM · {devices} dispositivos',
        'pt': '{count} ROMs · {devices} dispositivos', 'tr': '{count} ROM · {devices} cihaz',
        'id': '{count} ROM · {devices} perangkat', 'vi': '{count} ROM · {devices} thiết bị',
        'th': '{count} แพ็กเกจ · {devices} รุ่น', 'ar': '{count} حزمة · {devices} جهاز',
    },
    'hasRecovery': {
        'zh-cn': '有卡刷包', 'zh-tw': '有卡刷包', 'en-us': 'Recovery available', 'ja': 'リカバリあり',
        'ko': 'Recovery 있음', 'ru': 'Есть Recovery', 'uk': 'Є Recovery', 'pl': 'Jest Recovery',
        'de': 'Recovery vorhanden', 'fr': 'Recovery disponible', 'it': 'Recovery disponibile',
        'es': 'Recovery disponible', 'pt': 'Recovery disponível', 'tr': 'Recovery var',
        'id': 'Ada Recovery', 'vi': 'Có Recovery', 'th': 'มี Recovery', 'ar': 'يتوفر Recovery',
    },
    'noRecovery': {
        'zh-cn': '无卡刷包', 'zh-tw': '無卡刷包', 'en-us': 'No recovery', 'ja': 'リカバリなし',
        'ko': 'Recovery 없음', 'ru': 'Нет Recovery', 'uk': 'Немає Recovery', 'pl': 'Brak Recovery',
        'de': 'Kein Recovery', 'fr': 'Pas de Recovery', 'it': 'Nessun Recovery',
        'es': 'Sin Recovery', 'pt': 'Sem Recovery', 'tr': 'Recovery yok',
        'id': 'Tanpa Recovery', 'vi': 'Không có Recovery', 'th': 'ไม่มี Recovery', 'ar': 'لا يتوفر Recovery',
    },
    'hasFastboot': {
        'zh-cn': '有线刷包', 'zh-tw': '有線刷包', 'en-us': 'Fastboot available', 'ja': 'Fastboot あり',
        'ko': 'Fastboot 있음', 'ru': 'Есть Fastboot', 'uk': 'Є Fastboot', 'pl': 'Jest Fastboot',
        'de': 'Fastboot vorhanden', 'fr': 'Fastboot disponible', 'it': 'Fastboot disponibile',
        'es': 'Fastboot disponible', 'pt': 'Fastboot disponível', 'tr': 'Fastboot var',
        'id': 'Ada Fastboot', 'vi': 'Có Fastboot', 'th': 'มี Fastboot', 'ar': 'يتوفر Fastboot',
    },
    'noFastboot': {
        'zh-cn': '无线刷包', 'zh-tw': '無線刷包', 'en-us': 'No fastboot', 'ja': 'Fastboot なし',
        'ko': 'Fastboot 없음', 'ru': 'Нет Fastboot', 'uk': 'Немає Fastboot', 'pl': 'Brak Fastboot',
        'de': 'Kein Fastboot', 'fr': 'Pas de Fastboot', 'it': 'Nessun Fastboot',
        'es': 'Sin Fastboot', 'pt': 'Sem Fastboot', 'tr': 'Fastboot yok',
        'id': 'Tanpa Fastboot', 'vi': 'Không có Fastboot', 'th': 'ไม่มี Fastboot', 'ar': 'لا يتوفر Fastboot',
    },
    'loading': {
        'zh-cn': '加载中…', 'zh-tw': '載入中…', 'en-us': 'Loading…', 'ja': '読み込み中…',
        'ko': '불러오는 중…', 'ru': 'Загрузка…', 'uk': 'Завантаження…', 'pl': 'Ładowanie…',
        'de': 'Wird geladen…', 'fr': 'Chargement…', 'it': 'Caricamento…', 'es': 'Cargando…',
        'pt': 'Carregando…', 'tr': 'Yükleniyor…', 'id': 'Memuat…', 'vi': 'Đang tải…',
        'th': 'กำลังโหลด…', 'ar': 'جارٍ التحميل…',
    },
    'changelogFailed': {
        'zh-cn': '更新日志加载失败', 'zh-tw': '更新日誌載入失敗', 'en-us': 'Failed to load changelog',
        'ja': '更新履歴を読み込めません', 'ko': '변경 내역을 불러오지 못함', 'ru': 'Не удалось загрузить список изменений',
        'uk': 'Не вдалося завантажити список змін', 'pl': 'Nie udało się wczytać zmian',
        'de': 'Änderungsprotokoll konnte nicht geladen werden', 'fr': 'Échec du chargement du journal',
        'it': 'Impossibile caricare il changelog', 'es': 'No se pudo cargar el registro de cambios',
        'pt': 'Falha ao carregar o registro de alterações', 'tr': 'Değişiklik günlüğü yüklenemedi',
        'id': 'Gagal memuat log perubahan', 'vi': 'Không tải được nhật ký thay đổi',
        'th': 'โหลดบันทึกการเปลี่ยนแปลงไม่สำเร็จ', 'ar': 'تعذّر تحميل سجل التغييرات',
    },
    'noPackage': {
        'zh-cn': '该版本没有可下载的包', 'zh-tw': '該版本沒有可下載的包', 'en-us': 'No package for this version',
        'ja': 'このバージョンにパッケージはありません', 'ko': '이 버전에는 패키지가 없습니다',
        'ru': 'Для этой версии нет пакета', 'uk': 'Для цієї версії немає пакета',
        'pl': 'Brak pakietu dla tej wersji', 'de': 'Für diese Version gibt es kein Paket',
        'fr': 'Aucun paquet pour cette version', 'it': 'Nessun pacchetto per questa versione',
        'es': 'No hay paquete para esta versión', 'pt': 'Sem pacote para esta versão',
        'tr': 'Bu sürüm için paket yok', 'id': 'Tidak ada paket untuk versi ini',
        'vi': 'Không có gói cho phiên bản này', 'th': 'ไม่มีแพ็กเกจสำหรับเวอร์ชันนี้',
        'ar': 'لا توجد حزمة لهذا الإصدار',
    },
    'noPatch': {
        'zh-cn': '补丁未知', 'zh-tw': '修補程式未知', 'en-us': 'No patch info', 'ja': 'パッチ不明',
        'ko': '패치 정보 없음', 'ru': 'Патч неизвестен', 'uk': 'Патч невідомий', 'pl': 'Brak informacji o poprawce',
        'de': 'Kein Patch', 'fr': 'Correctif inconnu', 'it': 'Patch sconosciuto',
        'es': 'Parche desconocido', 'pt': 'Patch desconhecido', 'tr': 'Yama bilinmiyor',
        'id': 'Patch tidak diketahui', 'vi': 'Không rõ bản vá', 'th': 'ไม่ทราบแพตช์', 'ar': 'التصحيح غير معروف',
    },
    'highSpeed': {
        'zh-cn': '高速下载', 'zh-tw': '高速下載', 'en-us': 'High-speed download', 'ja': '高速ダウンロード',
        'ko': '고속 다운로드', 'ru': 'Ускоренная загрузка', 'uk': 'Швидке завантаження',
        'pl': 'Szybkie pobieranie', 'de': 'Schneller Download', 'fr': 'Téléchargement rapide',
        'it': 'Download veloce', 'es': 'Descarga rápida', 'pt': 'Download rápido',
        'tr': 'Hızlı indirme', 'id': 'Unduhan cepat', 'vi': 'Tải nhanh',
        'th': 'ดาวน์โหลดความเร็วสูง', 'ar': 'تنزيل عالي السرعة',
    },
    'getHighSpeed': {
        'zh-cn': '获取高速下载链接', 'zh-tw': '取得高速下載連結', 'en-us': 'Get high-speed link',
        'ja': '高速ダウンロードリンクを取得', 'ko': '고속 다운로드 링크 가져오기',
        'ru': 'Получить быструю ссылку', 'uk': 'Отримати швидке посилання',
        'pl': 'Pobierz szybki link', 'de': 'Schnellen Link abrufen',
        'fr': 'Obtenir le lien rapide', 'it': 'Ottieni link veloce',
        'es': 'Obtener enlace rápido', 'pt': 'Obter link rápido',
        'tr': 'Hızlı bağlantıyı al', 'id': 'Dapatkan tautan cepat',
        'vi': 'Lấy liên kết nhanh', 'th': 'รับลิงก์ความเร็วสูง', 'ar': 'الحصول على رابط سريع',
    },
    'fetching': {
        'zh-cn': '获取中…', 'zh-tw': '取得中…', 'en-us': 'Fetching…', 'ja': '取得中…',
        'ko': '가져오는 중…', 'ru': 'Получение…', 'uk': 'Отримання…', 'pl': 'Pobieranie…',
        'de': 'Wird abgerufen…', 'fr': 'Récupération…', 'it': 'Recupero…', 'es': 'Obteniendo…',
        'pt': 'Obtendo…', 'tr': 'Alınıyor…', 'id': 'Mengambil…', 'vi': 'Đang lấy…',
        'th': 'กำลังรับ…', 'ar': 'جارٍ الجلب…',
    },
    'requestFailed': {
        'zh-cn': '请求失败：', 'zh-tw': '請求失敗：', 'en-us': 'Request failed: ', 'ja': 'リクエスト失敗：',
        'ko': '요청 실패: ', 'ru': 'Ошибка запроса: ', 'uk': 'Помилка запиту: ', 'pl': 'Żądanie nie powiodło się: ',
        'de': 'Anfrage fehlgeschlagen: ', 'fr': 'Échec de la requête : ', 'it': 'Richiesta non riuscita: ',
        'es': 'Error en la solicitud: ', 'pt': 'Falha na solicitação: ', 'tr': 'İstek başarısız: ',
        'id': 'Permintaan gagal: ', 'vi': 'Yêu cầu thất bại: ', 'th': 'คำขอล้มเหลว: ', 'ar': 'فشل الطلب: ',
    },
    'copyLink': {
        'zh-cn': '复制链接', 'zh-tw': '複製連結', 'en-us': 'Copy link', 'ja': 'リンクをコピー',
        'ko': '링크 복사', 'ru': 'Копировать ссылку', 'uk': 'Копіювати посилання', 'pl': 'Kopiuj link',
        'de': 'Link kopieren', 'fr': 'Copier le lien', 'it': 'Copia link', 'es': 'Copiar enlace',
        'pt': 'Copiar link', 'tr': 'Bağlantıyı kopyala', 'id': 'Salin tautan',
        'vi': 'Sao chép liên kết', 'th': 'คัดลอกลิงก์', 'ar': 'نسخ الرابط',
    },
    'recentUpdates': {
        'zh-cn': '最近更新', 'zh-tw': '最近更新', 'en-us': 'Recent updates', 'ja': '最近の更新',
        'ko': '최근 업데이트', 'ru': 'Недавние обновления', 'uk': 'Останні оновлення',
        'pl': 'Ostatnie aktualizacje', 'de': 'Letzte Updates', 'fr': 'Mises à jour récentes',
        'it': 'Aggiornamenti recenti', 'es': 'Actualizaciones recientes',
        'pt': 'Atualizações recentes', 'tr': 'Son güncellemeler', 'id': 'Pembaruan terbaru',
        'vi': 'Cập nhật gần đây', 'th': 'อัปเดตล่าสุด', 'ar': 'أحدث التحديثات',
    },
    'updatedInDays': {
        'zh-cn': '近 {days} 日更新', 'zh-tw': '近 {days} 日更新', 'en-us': 'Updated in last {days} days',
        'ja': '過去 {days} 日の更新', 'ko': '최근 {days}일 업데이트', 'ru': 'Обновления за {days} дней',
        'uk': 'Оновлення за {days} днів', 'pl': 'Aktualizacje z {days} dni',
        'de': 'Updates der letzten {days} Tage', 'fr': 'Mises à jour des {days} derniers jours',
        'it': 'Aggiornamenti degli ultimi {days} giorni', 'es': 'Actualizaciones de los últimos {days} días',
        'pt': 'Atualizações dos últimos {days} dias', 'tr': 'Son {days} gündeki güncellemeler',
        'id': 'Pembaruan {days} hari terakhir', 'vi': 'Cập nhật trong {days} ngày qua',
        'th': 'อัปเดตใน {days} วันที่ผ่านมา', 'ar': 'تحديثات آخر {days} يومًا',
    },
    'romVersionsSuffix': {
        'zh-cn': '个 ROM 版本', 'zh-tw': '個 ROM 版本', 'en-us': 'ROM versions', 'ja': 'ROM バージョン',
        'ko': 'ROM 버전', 'ru': 'версий ROM', 'uk': 'версій ROM', 'pl': 'wersji ROM',
        'de': 'ROM-Versionen', 'fr': 'versions ROM', 'it': 'versioni ROM', 'es': 'versiones ROM',
        'pt': 'versões ROM', 'tr': 'ROM sürümü', 'id': 'versi ROM', 'vi': 'phiên bản ROM',
        'th': 'เวอร์ชัน ROM', 'ar': 'إصدار ROM',
    },
    'series': {
        'zh-cn': '系列', 'zh-tw': '系列', 'en-us': 'Series', 'ja': 'シリーズ', 'ko': '시리즈',
        'ru': 'Серия', 'uk': 'Серія', 'pl': 'Seria', 'de': 'Serie', 'fr': 'Série',
        'it': 'Serie', 'es': 'Serie', 'pt': 'Série', 'tr': 'Seri', 'id': 'Seri',
        'vi': 'Dòng', 'th': 'ซีรีส์', 'ar': 'السلسلة',
    },
    'appearance': {
        'zh-cn': '外观', 'zh-tw': '外觀', 'en-us': 'Appearance', 'ja': '外観', 'ko': '화면',
        'ru': 'Оформление', 'uk': 'Вигляд', 'pl': 'Wygląd', 'de': 'Darstellung', 'fr': 'Apparence',
        'it': 'Aspetto', 'es': 'Apariencia', 'pt': 'Aparência', 'tr': 'Görünüm', 'id': 'Tampilan',
        'vi': 'Giao diện', 'th': 'รูปลักษณ์', 'ar': 'المظهر',
    },
    'language': {
        'zh-cn': '语言', 'zh-tw': '語言', 'en-us': 'Language', 'ja': '言語', 'ko': '언어',
        'ru': 'Язык', 'uk': 'Мова', 'pl': 'Język', 'de': 'Sprache', 'fr': 'Langue',
        'it': 'Lingua', 'es': 'Idioma', 'pt': 'Idioma', 'tr': 'Dil', 'id': 'Bahasa',
        'vi': 'Ngôn ngữ', 'th': 'ภาษา', 'ar': 'اللغة',
    },
    'builtWith': {
        'zh-cn': '技术栈', 'zh-tw': '技術棧', 'en-us': 'Built with', 'ja': '技術スタック',
        'ko': '기술 스택', 'ru': 'Технологии', 'uk': 'Технології', 'pl': 'Technologie',
        'de': 'Technik', 'fr': 'Technologies', 'it': 'Tecnologie', 'es': 'Tecnologías',
        'pt': 'Tecnologias', 'tr': 'Teknolojiler', 'id': 'Teknologi', 'vi': 'Công nghệ',
        'th': 'เทคโนโลยี', 'ar': 'التقنيات',
    },
    'dataUpdatedAt': {
        'zh-cn': '数据更新时间：{time}', 'zh-tw': '資料更新時間：{time}', 'en-us': 'Data updated: {time}',
        'ja': 'データ更新日時：{time}', 'ko': '데이터 업데이트: {time}', 'ru': 'Данные обновлены: {time}',
        'uk': 'Дані оновлено: {time}', 'pl': 'Dane zaktualizowano: {time}',
        'de': 'Daten aktualisiert: {time}', 'fr': 'Données mises à jour : {time}',
        'it': 'Dati aggiornati: {time}', 'es': 'Datos actualizados: {time}',
        'pt': 'Dados atualizados: {time}', 'tr': 'Veriler güncellendi: {time}',
        'id': 'Data diperbarui: {time}', 'vi': 'Dữ liệu cập nhật: {time}',
        'th': 'ข้อมูลอัปเดต: {time}', 'ar': 'آخر تحديث للبيانات: {time}',
    },
    'aboutSource': {
        'zh-cn': '数据来自 data 仓库导出的 v3 JSON 接口，与网页端 hub.miuier.com 同源。',
        'zh-tw': '資料來自 data 倉庫匯出的 v3 JSON 介面，與網頁端 hub.miuier.com 同源。',
        'en-us': 'Data comes from the v3 JSON API exported by the data repository — the same source as hub.miuier.com.',
        'ja': 'データは data リポジトリが出力する v3 JSON API から取得しており、hub.miuier.com と同じソースです。',
        'ko': '데이터는 data 저장소가 내보내는 v3 JSON API에서 가져오며 hub.miuier.com과 같은 소스입니다.',
        'ru': 'Данные берутся из v3 JSON API, который публикует репозиторий data, — тот же источник, что и у hub.miuier.com.',
        'uk': 'Дані надходять із v3 JSON API, який публікує репозиторій data, — те саме джерело, що й у hub.miuier.com.',
        'pl': 'Dane pochodzą z interfejsu v3 JSON publikowanego przez repozytorium data — tego samego źródła co hub.miuier.com.',
        'de': 'Die Daten stammen aus der v3-JSON-API des data-Repositorys – dieselbe Quelle wie hub.miuier.com.',
        'fr': "Les données proviennent de l'API JSON v3 publiée par le dépôt data, la même source que hub.miuier.com.",
        'it': "I dati provengono dall'API JSON v3 pubblicata dal repository data, la stessa fonte di hub.miuier.com.",
        'es': 'Los datos provienen de la API JSON v3 publicada por el repositorio data, la misma fuente que hub.miuier.com.',
        'pt': 'Os dados vêm da API JSON v3 publicada pelo repositório data, a mesma fonte do hub.miuier.com.',
        'tr': 'Veriler data deposunun yayımladığı v3 JSON API’sinden gelir; hub.miuier.com ile aynı kaynaktır.',
        'id': 'Data berasal dari API JSON v3 yang diterbitkan repositori data, sumber yang sama dengan hub.miuier.com.',
        'vi': 'Dữ liệu đến từ API JSON v3 do kho data phát hành, cùng nguồn với hub.miuier.com.',
        'th': 'ข้อมูลมาจาก API JSON v3 ที่เผยแพร่โดยรีโพซิทอรี data ซึ่งเป็นแหล่งเดียวกับ hub.miuier.com',
        'ar': 'تأتي البيانات من واجهة v3 JSON التي ينشرها مستودع data، وهي المصدر نفسه لموقع hub.miuier.com.',
    },
    'appDisclaimer': {
        'zh-cn': '第三方非官方应用，与小米公司及 MIUI / HyperOS 开发团队无任何关联。',
        'zh-tw': '第三方非官方應用程式，與小米公司及 MIUI / HyperOS 開發團隊無任何關聯。',
        'en-us': 'Unofficial third-party app, not affiliated with Xiaomi Inc. or the MIUI / HyperOS team.',
        'ja': '非公式のサードパーティ製アプリです。Xiaomi 社および MIUI / HyperOS 開発チームとは無関係です。',
        'ko': '비공식 서드파티 앱으로 Xiaomi 및 MIUI / HyperOS 개발팀과 무관합니다.',
        'ru': 'Неофициальное стороннее приложение, не связано с Xiaomi Inc. и командой MIUI / HyperOS.',
        'uk': 'Неофіційний сторонній застосунок, не пов’язаний із Xiaomi Inc. чи командою MIUI / HyperOS.',
        'pl': 'Nieoficjalna aplikacja osób trzecich, niezwiązana z Xiaomi Inc. ani zespołem MIUI / HyperOS.',
        'de': 'Inoffizielle Drittanbieter-App, nicht mit Xiaomi Inc. oder dem MIUI-/HyperOS-Team verbunden.',
        'fr': "Application tierce non officielle, sans lien avec Xiaomi Inc. ni l'équipe MIUI / HyperOS.",
        'it': 'App di terze parti non ufficiale, non affiliata a Xiaomi Inc. né al team MIUI / HyperOS.',
        'es': 'Aplicación no oficial de terceros, sin relación con Xiaomi Inc. ni con el equipo de MIUI / HyperOS.',
        'pt': 'Aplicativo não oficial de terceiros, sem vínculo com a Xiaomi Inc. ou a equipe MIUI / HyperOS.',
        'tr': 'Resmî olmayan üçüncü taraf uygulama; Xiaomi Inc. veya MIUI / HyperOS ekibiyle bağlantılı değildir.',
        'id': 'Aplikasi pihak ketiga tidak resmi, tidak berafiliasi dengan Xiaomi Inc. atau tim MIUI / HyperOS.',
        'vi': 'Ứng dụng bên thứ ba không chính thức, không liên kết với Xiaomi Inc. hay nhóm MIUI / HyperOS.',
        'th': 'แอปบุคคลที่สามอย่างไม่เป็นทางการ ไม่เกี่ยวข้องกับ Xiaomi Inc. หรือทีม MIUI / HyperOS',
        'ar': 'تطبيق غير رسمي من طرف ثالث، لا صلة له بشركة Xiaomi Inc. أو فريق MIUI / HyperOS.',
    },
    'otaOutdated': {
        'zh-cn': '该版本已不是最新版（最新为 {latest}），小米只对最新版签发高速链接。',
        'zh-tw': '該版本已不是最新版（最新為 {latest}），小米只對最新版簽發高速連結。',
        'en-us': 'Not the newest build (latest is {latest}); Xiaomi only signs the newest build.',
        'ja': '最新版ではありません（最新は {latest}）。小米は最新版にのみ高速リンクを発行します。',
        'ko': '최신 버전이 아닙니다(최신: {latest}). 샤오미는 최신 버전에만 고속 링크를 발급합니다.',
        'ru': 'Это не последняя сборка (последняя — {latest}); Xiaomi подписывает только её.',
        'uk': 'Це не остання збірка (остання — {latest}); Xiaomi підписує лише її.',
        'pl': 'To nie najnowsza kompilacja (najnowsza: {latest}); Xiaomi podpisuje tylko najnowszą.',
        'de': 'Nicht die neueste Version (neueste: {latest}); Xiaomi signiert nur diese.',
        'fr': "Ce n'est pas la dernière version ({latest}) ; Xiaomi ne signe que la dernière.",
        'it': "Non è l'ultima versione (ultima: {latest}); Xiaomi firma solo l'ultima.",
        'es': 'No es la última versión (la última es {latest}); Xiaomi solo firma la última.',
        'pt': 'Não é a versão mais recente ({latest}); a Xiaomi só assina a mais recente.',
        'tr': 'En son sürüm değil (en son: {latest}); Xiaomi yalnızca en son sürümü imzalar.',
        'id': 'Bukan versi terbaru (terbaru: {latest}); Xiaomi hanya menandatangani versi terbaru.',
        'vi': 'Không phải bản mới nhất (mới nhất: {latest}); Xiaomi chỉ ký bản mới nhất.',
        'th': 'ไม่ใช่เวอร์ชันล่าสุด (ล่าสุด: {latest}) Xiaomi ออกลิงก์ให้เฉพาะเวอร์ชันล่าสุด',
        'ar': 'ليس أحدث إصدار (الأحدث: {latest})؛ توقّع Xiaomi على أحدث إصدار فقط.',
    },
    'otaUnsigned': {
        'zh-cn': '接口未返回签名，直链会被拒绝，请用上面的 CDN 地址。',
        'zh-tw': '介面未回傳簽章，直連會被拒絕，請用上面的 CDN 位址。',
        'en-us': 'The API returned no signature, so the direct link would be rejected — use the CDN link above.',
        'ja': '署名が返らなかったため直リンクは拒否されます。上の CDN リンクをご利用ください。',
        'ko': '서명이 반환되지 않아 직링크는 거부됩니다. 위의 CDN 링크를 사용하세요.',
        'ru': 'Подпись не получена, прямая ссылка будет отклонена — используйте ссылку CDN выше.',
        'uk': 'Підпис не отримано, пряме посилання буде відхилено — скористайтеся посиланням CDN вище.',
        'pl': 'Brak podpisu, więc link bezpośredni zostanie odrzucony — użyj linku CDN powyżej.',
        'de': 'Keine Signatur erhalten, der Direktlink würde abgelehnt – nutze den CDN-Link oben.',
        'fr': 'Aucune signature renvoyée : le lien direct serait refusé — utilisez le lien CDN ci-dessus.',
        'it': 'Nessuna firma restituita: il link diretto verrebbe rifiutato — usa il link CDN sopra.',
        'es': 'La API no devolvió firma, el enlace directo sería rechazado: usa el enlace CDN de arriba.',
        'pt': 'A API não retornou assinatura; o link direto seria recusado — use o link CDN acima.',
        'tr': 'İmza dönmediği için doğrudan bağlantı reddedilir — yukarıdaki CDN bağlantısını kullanın.',
        'id': 'API tidak mengembalikan tanda tangan, tautan langsung akan ditolak — gunakan tautan CDN di atas.',
        'vi': 'API không trả về chữ ký nên liên kết trực tiếp sẽ bị từ chối — hãy dùng liên kết CDN ở trên.',
        'th': 'API ไม่ได้ส่งลายเซ็นกลับมา ลิงก์ตรงจะถูกปฏิเสธ โปรดใช้ลิงก์ CDN ด้านบน',
        'ar': 'لم تُرجع الواجهة توقيعًا، لذا سيُرفض الرابط المباشر — استخدم رابط CDN أعلاه.',
    },
    'otaNotFound': {
        'zh-cn': '小米接口没有这个版本的信息（老机型或已下架版本常见）。',
        'zh-tw': '小米介面沒有這個版本的資訊（老機型或已下架版本常見）。',
        'en-us': "Xiaomi's API has no information for this version (common for old or delisted builds).",
        'ja': '小米の接口にこのバージョンの情報がありません（旧機種や配信終了版でよくあります）。',
        'ko': '샤오미 API에 이 버전의 정보가 없습니다(오래된 기기나 배포 종료 버전에서 흔함).',
        'ru': 'В API Xiaomi нет данных об этой версии (обычно для старых или снятых сборок).',
        'uk': 'В API Xiaomi немає інформації про цю версію (типово для старих або знятих збірок).',
        'pl': 'API Xiaomi nie ma informacji o tej wersji (częste dla starych lub wycofanych kompilacji).',
        'de': 'Die Xiaomi-API hat keine Informationen zu dieser Version (häufig bei alten Versionen).',
        'fr': "L'API Xiaomi n'a pas d'informations pour cette version (fréquent pour les anciennes).",
        'it': "L'API Xiaomi non ha informazioni per questa versione (comune per versioni vecchie).",
        'es': 'La API de Xiaomi no tiene información de esta versión (habitual en versiones antiguas).',
        'pt': 'A API da Xiaomi não tem informações desta versão (comum em versões antigas).',
        'tr': "Xiaomi API'sinde bu sürüm için bilgi yok (eski sürümlerde sık görülür).",
        'id': 'API Xiaomi tidak punya informasi untuk versi ini (umum pada versi lama).',
        'vi': 'API của Xiaomi không có thông tin cho phiên bản này (thường gặp ở bản cũ).',
        'th': 'API ของ Xiaomi ไม่มีข้อมูลเวอร์ชันนี้ (พบบ่อยในรุ่นเก่าหรือที่เลิกให้บริการ)',
        'ar': 'لا تتوفر معلومات في واجهة Xiaomi لهذا الإصدار (شائع في الإصدارات القديمة).',
    },
    'expand': {
        'zh-cn': '展开', 'zh-tw': '展開', 'en-us': 'Expand', 'ja': '展開', 'ko': '펼치기',
        'ru': 'Развернуть', 'uk': 'Розгорнути', 'pl': 'Rozwiń', 'de': 'Erweitern',
        'fr': 'Développer', 'it': 'Espandi', 'es': 'Expandir', 'pt': 'Expandir',
        'tr': 'Genişlet', 'id': 'Bentangkan', 'vi': 'Mở rộng', 'th': 'ขยาย', 'ar': 'توسيع',
    },
    'collapse': {
        'zh-cn': '收起', 'zh-tw': '收合', 'en-us': 'Collapse', 'ja': '折りたたむ', 'ko': '접기',
        'ru': 'Свернуть', 'uk': 'Згорнути', 'pl': 'Zwiń', 'de': 'Einklappen',
        'fr': 'Réduire', 'it': 'Comprimi', 'es': 'Contraer', 'pt': 'Recolher',
        'tr': 'Daralt', 'id': 'Runtuhkan', 'vi': 'Thu gọn', 'th': 'ย่อ', 'ar': 'طيّ',
    },
    # 主题模式是三个并排的 tab，标签太长会被截断，所以各语言都取短词
    'themeSystem': {
        'zh-cn': '跟随系统', 'zh-tw': '跟隨系統', 'en-us': 'System', 'ja': 'システム',
        'ko': '시스템', 'ru': 'Система', 'uk': 'Система', 'pl': 'System',
        'de': 'System', 'fr': 'Système', 'it': 'Sistema',
        'es': 'Sistema', 'pt': 'Sistema', 'tr': 'Sistem',
        'id': 'Sistem', 'vi': 'Hệ thống', 'th': 'ระบบ', 'ar': 'النظام',
    },
    'themeLight': {
        'zh-cn': '浅色', 'zh-tw': '淺色', 'en-us': 'Light', 'ja': 'ライト', 'ko': '라이트',
        'ru': 'Светлая', 'uk': 'Світла', 'pl': 'Jasny', 'de': 'Hell', 'fr': 'Clair',
        'it': 'Chiaro', 'es': 'Claro', 'pt': 'Claro', 'tr': 'Açık', 'id': 'Terang',
        'vi': 'Sáng', 'th': 'สว่าง', 'ar': 'فاتح',
    },
    'romCountOnly': {
        'zh-cn': '{count} 个包', 'zh-tw': '{count} 個包', 'en-us': '{count} ROMs',
        'ja': '{count} 個', 'ko': '{count}개', 'ru': '{count} пакетов', 'uk': '{count} пакетів',
        'pl': '{count} pakietów', 'de': '{count} Pakete', 'fr': '{count} ROM',
        'it': '{count} ROM', 'es': '{count} ROM', 'pt': '{count} ROMs',
        'tr': '{count} ROM', 'id': '{count} ROM', 'vi': '{count} ROM',
        'th': '{count} แพ็กเกจ', 'ar': '{count} حزمة',
    },
    'backToTop': {
        'zh-cn': '回到顶部', 'zh-tw': '回到頂部', 'en-us': 'Back to top', 'ja': 'トップへ戻る',
        'ko': '맨 위로', 'ru': 'Наверх', 'uk': 'Догори', 'pl': 'Do góry',
        'de': 'Nach oben', 'fr': 'Haut de page', 'it': 'Torna su',
        'es': 'Volver arriba', 'pt': 'Voltar ao topo', 'tr': 'Başa dön',
        'id': 'Kembali ke atas', 'vi': 'Về đầu trang', 'th': 'กลับขึ้นด้านบน', 'ar': 'العودة إلى الأعلى',
    },
    'loadFailed': {
        'zh-cn': '加载失败', 'zh-tw': '載入失敗', 'en-us': 'Failed to load', 'ja': '読み込み失敗',
        'ko': '불러오기 실패', 'ru': 'Не удалось загрузить', 'uk': 'Не вдалося завантажити',
        'pl': 'Nie udało się wczytać', 'de': 'Laden fehlgeschlagen', 'fr': 'Échec du chargement',
        'it': 'Caricamento non riuscito', 'es': 'Error al cargar', 'pt': 'Falha ao carregar',
        'tr': 'Yüklenemedi', 'id': 'Gagal memuat', 'vi': 'Tải thất bại',
        'th': 'โหลดไม่สำเร็จ', 'ar': 'فشل التحميل',
    },
    'retry': {
        'zh-cn': '重试', 'zh-tw': '重試', 'en-us': 'Retry', 'ja': '再試行', 'ko': '다시 시도',
        'ru': 'Повторить', 'uk': 'Повторити', 'pl': 'Ponów', 'de': 'Erneut versuchen',
        'fr': 'Réessayer', 'it': 'Riprova', 'es': 'Reintentar', 'pt': 'Tentar novamente',
        'tr': 'Yeniden dene', 'id': 'Coba lagi', 'vi': 'Thử lại', 'th': 'ลองใหม่', 'ar': 'إعادة المحاولة',
    },
    'themeDark': {
        'zh-cn': '深色', 'zh-tw': '深色', 'en-us': 'Dark', 'ja': 'ダーク', 'ko': '다크',
        'ru': 'Тёмная', 'uk': 'Темна', 'pl': 'Ciemny', 'de': 'Dunkel', 'fr': 'Sombre',
        'it': 'Scuro', 'es': 'Oscuro', 'pt': 'Escuro', 'tr': 'Koyu', 'id': 'Gelap',
        'vi': 'Tối', 'th': 'มืด', 'ar': 'داكن',
    },
}


def load_web_locales():
    """用 node 直接求值网页端的语言包（它们是纯字面量对象，没有 TS 类型标注）"""
    script = '''
import { readFile, readdir, writeFile } from 'fs/promises'
import { join, basename } from 'path'
const dir = process.argv[2]
const files = (await readdir(dir)).filter(f => f.endsWith('.ts')).sort()
const out = {}
for (const f of files) {
  const src = await readFile(join(dir, f), 'utf-8')
  const mod = await import(`data:text/javascript;base64,${Buffer.from(src).toString('base64')}`)
  out[basename(f, '.ts')] = mod.default
}
await writeFile(process.argv[3], JSON.stringify(out))
'''
    tmp_js = '/tmp/_dump_locales.mjs'
    tmp_json = '/tmp/_locales.json'
    Path(tmp_js).write_text(script, encoding='utf-8')
    subprocess.run(['node', tmp_js, str(WEB_LOCALES), tmp_json], check=True)
    return json.loads(Path(tmp_json).read_text(encoding='utf-8'))


def kotlin_str(value):
    """转义成 Kotlin 字符串字面量：$ 在 Kotlin 里是模板起始符，必须转义"""
    out = value.replace('\\', '\\\\').replace('"', '\\"').replace('$', '\\$')
    return f'"{out}"'


def main():
    web = load_web_locales()
    codes = [c for _, c, _ in LANGS]
    missing = [c for c in codes if c not in web]
    if missing:
        sys.exit(f'网页端缺少这些语言包：{missing}')

    lines = [
        '// 由 scripts/make_locales.py 生成，不要手改。',
        '//',
        '// 语言列表与词条来源对齐网页端 app/web/i18n/locales/*.ts：',
        '//   - 通用 UI 词条（下载 / 更新日志 / 品牌 / 支持系统 / 关于 / 免责声明 …）',
        '//     以及全部区域名与运营商名直接取自网页端语言包',
        '//   - 只在本 App 出现的词条在生成脚本里维护',
        'package com.miuier.hub.ui.i18n',
        '',
        'import com.miuier.hub.data.AppLang',
        '',
        '/** 某种语言的全部本地化数据 */',
        'data class LocaleData(',
        '    val strings: Strings,',
        '    val regions: Map<String, String>,',
        '    val carriers: Map<String, String>,',
        ')',
        '',
        'val Locales: Map<AppLang, LocaleData> = mapOf(',
    ]

    for enum_name, code, _ in LANGS:
        msgs = web[code]
        lines.append(f'    AppLang.{enum_name} to LocaleData(')
        lines.append('        strings = Strings(')
        for key, web_key in REUSE.items():
            lines.append(f'            {key} = {kotlin_str(msgs.get(web_key, ""))},')
        for key, table in APP_STRINGS.items():
            lines.append(f'            {key} = {kotlin_str(table[code])},')
        lines.append('        ),')
        for name, src in (('regions', 'regions'), ('carriers', 'carriers')):
            lines.append(f'        {name} = mapOf(')
            for k, v in msgs[src].items():
                lines.append(f'            {kotlin_str(k)} to {kotlin_str(v)},')
            lines.append('        ),')
        lines.append('    ),')

    lines.append(')')
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text('\n'.join(lines) + '\n', encoding='utf-8')
    print(f'已生成 {OUT.relative_to(HERE.parent.parent.parent)}')
    print(f'  语言 {len(LANGS)} 种，复用网页端词条 {len(REUSE)} 个，自有词条 {len(APP_STRINGS)} 个')


if __name__ == '__main__':
    main()
