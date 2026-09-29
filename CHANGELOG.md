# Changelog

## [1.1.1](https://github.com/renatoganske/gestao-de-eventos/compare/v1.1.0...v1.1.1) (2026-09-29)


### Bug Fixes

* corrige xpath do release-please e alinha pom.xml em 1.1.0 ([101e108](https://github.com/renatoganske/gestao-de-eventos/commit/101e10814f5544547e010190434570b04db3c5c6))

## [1.1.0](https://github.com/renatoganske/gestao-de-eventos/compare/v1.0.0...v1.1.0) (2026-09-29)


### Features

* **GDE-21:** design system base do frontend ([#38](https://github.com/renatoganske/gestao-de-eventos/issues/38)) ([2f7da54](https://github.com/renatoganske/gestao-de-eventos/commit/2f7da5412cbaaf35f29401eac765c6445e8b26d2))
* **GDE-22:** tela de Dashboard ([#39](https://github.com/renatoganske/gestao-de-eventos/issues/39)) ([d3e1def](https://github.com/renatoganske/gestao-de-eventos/commit/d3e1def8dc972d710fef612136cf428843847afe))
* **GDE-23:** tela de Eventos (busca + listagem unificada) ([#40](https://github.com/renatoganske/gestao-de-eventos/issues/40)) ([c6a7ad1](https://github.com/renatoganske/gestao-de-eventos/commit/c6a7ad18d30262231471e35ce261d8a0cc5882ca))
* **GDE-24:** tela de cadastro/edicao de evento ([#41](https://github.com/renatoganske/gestao-de-eventos/issues/41)) ([4ec44f5](https://github.com/renatoganske/gestao-de-eventos/commit/4ec44f51cff6520e95426ab4af33f76e47f327d0))
* **GDE-25:** tela de HDs (lista + alerta de capacidade) ([#42](https://github.com/renatoganske/gestao-de-eventos/issues/42)) ([f30fe2b](https://github.com/renatoganske/gestao-de-eventos/commit/f30fe2b166dbb112905610c2ea5bc88ad6fd0b65))
* **GDE-26:** telas de Clientes e Locais de Evento (lista + CRUD em modal) ([#43](https://github.com/renatoganske/gestao-de-eventos/issues/43)) ([e7b2e2c](https://github.com/renatoganske/gestao-de-eventos/commit/e7b2e2c001062127ea38d77cfb7097e41e83d4dd))
* **GDE-32:** tela de login e gestao de sessao JWT no frontend ([#37](https://github.com/renatoganske/gestao-de-eventos/issues/37)) ([f5c2602](https://github.com/renatoganske/gestao-de-eventos/commit/f5c26024a87d0b62c13ab8a6165ae3fe827db52e))
* **GDE-36:** tela de Profissionais (CRUD) no frontend ([#47](https://github.com/renatoganske/gestao-de-eventos/issues/47)) ([0430122](https://github.com/renatoganske/gestao-de-eventos/commit/043012277071531c59b446ce966a83a4d7afb722))
* **GDE:** escreve a associacao Event&lt;-&gt;Professional (ADR-0021) ([#45](https://github.com/renatoganske/gestao-de-eventos/issues/45)) ([74d10c0](https://github.com/renatoganske/gestao-de-eventos/commit/74d10c0452d92d059099c8a03f1f47b8de058a5c))
* **GDE:** Professional.type e specialty viram lookup/tags (ADR-0022) ([#46](https://github.com/renatoganske/gestao-de-eventos/issues/46)) ([2a388a8](https://github.com/renatoganske/gestao-de-eventos/commit/2a388a8d489c7c6a1b39efdfa9c42185c8442a58))


### Bug Fixes

* corrige build do frontend no deploy (client.test.ts) ([fd3492b](https://github.com/renatoganske/gestao-de-eventos/commit/fd3492b81fe419cd0ae13cb287c5befe080215b8))
* corrige tipagem do client.test.ts que quebrava o tsc -b no build ([#52](https://github.com/renatoganske/gestao-de-eventos/issues/52)) ([010b742](https://github.com/renatoganske/gestao-de-eventos/commit/010b7427e77aacdab2e858ec2b34536c52946302))
* responde 409 em violacao de integridade e trata no front ([#49](https://github.com/renatoganske/gestao-de-eventos/issues/49)) ([08fa0aa](https://github.com/renatoganske/gestao-de-eventos/commit/08fa0aae82907b1d737a2991bb941b31d3077426))
