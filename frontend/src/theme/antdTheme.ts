import { theme } from 'ant-design-vue'

// 全局唯一强调色（与 tokens.css 的 --cp-accent 必须一致）
const ACCENT = '#ff5c20'
const ACCENT_HOVER = '#ff7c4c'
const ACCENT_ACTIVE = '#b23c11'

export const antdTheme = {
  algorithm: theme.darkAlgorithm,
  token: {
    // 品牌
    colorPrimary: ACCENT,
    colorInfo: ACCENT,
    colorLink: ACCENT,
    colorLinkHover: ACCENT_HOVER,
    colorLinkActive: ACCENT_ACTIVE,
    colorSuccess: '#22c55e',
    colorWarning: '#ffb020',
    colorError: '#ff4d4f',

    // 背景
    colorBgBase: '#0b0f14',
    colorBgLayout: '#0b0f14',
    colorBgContainer: '#121821',
    colorBgElevated: '#18202b',
    colorBgContainerDisabled: '#151b23',
    colorBgTextHover: 'rgba(255, 92, 32, 0.08)',
    colorBgTextActive: 'rgba(255, 92, 32, 0.14)',

    // 填充：表格表头 / 行 hover 全部来自这两个
    colorFillAlter: 'rgba(255, 92, 32, 0.04)',
    colorFillAlterSolid: '#1b2634',
    colorFillSecondary: 'rgba(255, 92, 32, 0.08)',
    colorFillTertiary: 'rgba(255, 92, 32, 0.05)',
    colorFillContent: 'rgba(255, 255, 255, 0.04)',
    colorFillContentHover: 'rgba(255, 92, 32, 0.10)',

    // 描边 / 分割线
    colorBorder: '#223044',
    colorBorderSecondary: '#1a2431',
    colorBorderBg: '#0b0f14',
    colorSplit: '#1a2431',

    // 文字
    colorText: '#e6edf3',
    colorTextHeading: '#e6edf3',
    colorTextSecondary: '#9fb0c3',
    colorTextTertiary: '#6b7c91',
    colorTextQuaternary: '#5b6b7f',
    colorTextDescription: '#8b9bad',
    // 占位符属于正文文本：对 #121821 约 5.3:1，能过 AA
    colorTextPlaceholder: '#7d8ea3',
    colorTextDisabled: '#41505f',
    colorTextLightSolid: '#100803',
    colorIcon: '#9fb0c3',
    colorIconHover: '#e6edf3',

    // 焦点 / 阴影：一律收成 1px 硬边，不用柔和投影
    controlOutline: 'rgba(255, 92, 32, 0.20)',
    boxShadow: '0 0 0 1px #2f4a63',
    boxShadowSecondary: '0 0 0 1px #223044',
    boxShadowTertiary: '0 0 0 1px #1a2431',

    // 形状：直角 / 极小切角
    borderRadius: 2,
    borderRadiusLG: 2,
    borderRadiusSM: 2,
    wireframe: false,
    fontFamily:
      "'JetBrains Mono','Cascadia Code','PingFang SC','Microsoft YaHei',ui-monospace,monospace",
  },
  components: {
    Layout: {
      colorBgHeader: '#0b0f14',
      colorBgBody: '#0b0f14',
      colorBgTrigger: '#121821',
    },
    Menu: {
      colorItemBg: 'transparent',
      colorItemBgHover: 'rgba(255, 92, 32, 0.08)',
      colorItemBgSelected: 'rgba(255, 92, 32, 0.12)',
      colorItemBgSelectedHorizontal: 'transparent',
      colorItemText: '#c7d3e0',
      colorItemTextHover: '#e6edf3',
      colorItemTextSelected: ACCENT,
      colorItemTextSelectedHorizontal: ACCENT,
      colorActiveBarHeight: 1,
      colorActiveBarWidth: 0,
      colorActiveBarBorderSize: 0,
      radiusItem: 0,
      itemMarginInline: 8,
    },
  },
}