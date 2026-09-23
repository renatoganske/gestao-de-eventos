// ADR-0012: build estático do frontend vira artefato único junto do backend
// (gestao-de-eventos/src/main/resources/static), sem CORS nem host separado em produção.
import { cpSync, existsSync, rmSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const frontendRoot = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const distDir = resolve(frontendRoot, 'dist')
const staticDir = resolve(frontendRoot, '../gestao-de-eventos/src/main/resources/static')

if (!existsSync(distDir)) {
  console.error(`dist/ não encontrado em ${distDir} — rode "npm run build" antes.`)
  process.exit(1)
}

rmSync(staticDir, { recursive: true, force: true })
cpSync(distDir, staticDir, { recursive: true })

console.log(`Build copiado para ${staticDir}`)
