#!/bin/bash
# ==============================================
# IceHost 自动续期脚本 (API 版)
# ==============================================

API_KEY=""
SERVER_ID=""
RENEW_URL="https://dash.icehost.pl/api/client/freeservers/${SERVER_ID}/renew"
USER_AGENT="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36"

echo "renew.sh 已启动（IceHost API 自动续期模式）"
echo "🔁 每 6 小时自动执行一次续期请求..."

while true; do
  echo "[$(date)] 🔄 开始续期..."

  HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$RENEW_URL" \
    -H "Accept: application/json" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $API_KEY" \
    -H "User-Agent: $USER_AGENT")

  if [ "$HTTP_CODE" -eq 200 ]; then
    echo "[$(date)] ✅ 续期成功"
  elif [ "$HTTP_CODE" -eq 400 ]; then
    echo "[$(date)] ⚠️ 请求无效 (HTTP 400)，可能今日已续期"
  elif [ "$HTTP_CODE" -eq 404 ]; then
    echo "[$(date)] ⚠️ 未找到服务器 (HTTP 404)"
  elif [ "$HTTP_CODE" -eq 419 ]; then
    echo "[$(date)] ⚠️ 授权过期或无效 (HTTP 419)"
  elif [ "$HTTP_CODE" -eq 403 ]; then
    echo "[$(date)] ❌ 无访问权限 (HTTP 403)"
  else
    echo "[$(date)] ⚠️ 续期失败，返回码: $HTTP_CODE"
  fi

  echo "[$(date)] ⏰ 等待 6 小时后再次执行..."
  sleep 21600
done
