import 'vuetify/styles'
import '@mdi/font/css/materialdesignicons.css'
import { createVuetify } from 'vuetify'
import { aliases, mdi } from 'vuetify/iconsets/mdi'

export default createVuetify({
  icons: {
    defaultSet: 'mdi',
    aliases,
    sets: { mdi },
  },
  theme: {
    defaultTheme: 'clinic',
    themes: {
      clinic: {
        dark: false,
        colors: {
          primary: '#1565C0',
          secondary: '#42A5F5',
          accent: '#FF7043',
          success: '#66BB6A',
          warning: '#FFA726',
          error: '#EF5350',
          info: '#29B6F6',
          background: '#FAFAFA',
          surface: '#FFFFFF',
        },
      },
    },
  },
})
