#!/bin/sh
# Usar 'set -e' faz com que o script pare imediatamente se algum comando falhar.
set -e

# Imprime informações úteis para o log
echo '--- Variáveis de Ambiente do Container ---'
printenv
echo '--- Iniciando a Aplicação em Modo Debug ---'

# O comando 'exec' é crucial aqui. Ele substitui o processo do shell
# pelo processo do Gradle. Isso garante que sua aplicação Java receba
# corretamente os sinais do Docker (como o de parada, ex: docker stop).
exec ./gradlew bootRun -Pdebug -t --no-daemon

#Para o modo debug ativado adicionar esse parâmetro: -Pdebug