# Termux pkg itself depends on curl: use apt-get until curl is healthy.
[ "${PREFIX:-}" = /data/data/com.termux/files/usr ] || { echo 'Abra no Termux oficial.'; exit 1; }
export DEBIAN_FRONTEND=noninteractive
printf 'Maestro: atualizando os pacotes do Termux antes do download…\n'
apt-get update
apt-get -y -o Dpkg::Options::=--force-confdef -o Dpkg::Options::=--force-confold dist-upgrade
apt-get -y -o Dpkg::Options::=--force-confdef -o Dpkg::Options::=--force-confold install curl openssl libcurl
if ! curl --version >/dev/null 2>&1; then
 apt-get -y -o Dpkg::Options::=--force-confdef -o Dpkg::Options::=--force-confold --reinstall install curl openssl libcurl
fi
curl --version >/dev/null
