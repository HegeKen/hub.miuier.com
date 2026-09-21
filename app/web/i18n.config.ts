// 语言包聚合入口：各语言的词条在 ./i18n/locales/<code>.ts 中维护，新增语言只需
// 1) 复制一个语言包文件 2) 在此处 import 并登记 3) 在 nuxt.config.ts 的 i18n.locales 中登记
import zhCn from './i18n/locales/zh-cn'
import zhTw from './i18n/locales/zh-tw'
import enUs from './i18n/locales/en-us'
import ja from './i18n/locales/ja'
import ko from './i18n/locales/ko'
import ru from './i18n/locales/ru'
import uk from './i18n/locales/uk'
import pl from './i18n/locales/pl'
import de from './i18n/locales/de'
import fr from './i18n/locales/fr'
import it from './i18n/locales/it'
import es from './i18n/locales/es'
import pt from './i18n/locales/pt'
import tr from './i18n/locales/tr'
import id from './i18n/locales/id'
import vi from './i18n/locales/vi'
import th from './i18n/locales/th'
import ar from './i18n/locales/ar'
import hi from './i18n/locales/hi'
import ug from './i18n/locales/ug'
import bo from './i18n/locales/bo'

export default defineI18nConfig(() => ({
  legacy: false,
  locale: 'zh-cn',
  // 未翻译的词条回退到英文，避免出现裸 key
  fallbackLocale: 'en-us',
  messages: {
    'zh-cn': zhCn,
    'zh-tw': zhTw,
    'en-us': enUs,
    ja,
    ko,
    ru,
    uk,
    pl,
    de,
    fr,
    it,
    es,
    pt,
    tr,
    id,
    vi,
    th,
    ar,
    hi,
    ug,
    bo,
    // 兼容既有链接（/zh/、/en/），实际词条与 zh-cn / en-us 相同，不在语言切换器中显示
    zh: zhCn,
    en: enUs,
  },
}))
