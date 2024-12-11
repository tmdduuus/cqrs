# 환경 변수 설정 부분 수정
setup_environment() {
    USERID=$1
    NAME="${USERID}-cqrs"
    RESOURCE_GROUP="tiu-dgga-rg"
    DB_NAMESPACE="${USERID}-database"
    APP_NAMESPACE="${USERID}-application"

    # Event Hub 관련 환경변수 추가
    STORAGE_ACCOUNT="${USERID}storage"
    BLOB_CONTAINER="${USERID}-eventhub-checkpoints"
    PLAN_EVENT_HUB_NS="${USERID}-eventhub-plan-ns"
    USAGE_EVENT_HUB_NS="${USERID}-eventhub-usage-ns"
    EVENT_HUB_NAME="phone-plan-events"
    PLAN_HUB_NAME="${EVENT_HUB_NAME}-plan"
    USAGE_HUB_NAME="${EVENT_HUB_NAME}-usage"
}

# Storage 정리 함수 수정
cleanup_storage() {
    log "Blob Storage 정리 중..."

    # Storage Account의 연결 문자열 가져오기
    STORAGE_CONNECTION_STRING=$(az storage account show-connection-string \
        --name $STORAGE_ACCOUNT \
        --resource-group $RESOURCE_GROUP \
        --query connectionString \
        --output tsv)

    # 특정 사용자의 Blob Container 삭제
    az storage container delete \
        --name $BLOB_CONTAINER \
        --connection-string "$STORAGE_CONNECTION_STRING" \
        --if-exists \
        2>/dev/null || true

    log "Blob Storage 정리 완료"
}

# Event Hub 정리 함수 추가
cleanup_event_hub() {
    log "Event Hub 정리 중..."

    # Plan Event Hub 삭제
    az eventhubs eventhub delete \
        --name $PLAN_HUB_NAME \
        --namespace-name $PLAN_EVENT_HUB_NS \
        --resource-group $RESOURCE_GROUP \
        2>/dev/null || true

    # Usage Event Hub 삭제
    az eventhubs eventhub delete \
        --name $USAGE_HUB_NAME \
        --namespace-name $USAGE_EVENT_HUB_NS \
        --resource-group $RESOURCE_GROUP \
        2>/dev/null || true

    # Plan Event Hub 네임스페이스 삭제
    az eventhubs namespace delete \
        --name $PLAN_EVENT_HUB_NS \
        --resource-group $RESOURCE_GROUP \
        2>/dev/null || true

    # Usage Event Hub 네임스페이스 삭제
    az eventhubs namespace delete \
        --name $USAGE_EVENT_HUB_NS \
        --resource-group $RESOURCE_GROUP \
        2>/dev/null || true

    log "Event Hub 정리 완료"
}

# 메인 실행 함수 수정
main() {
    log "CQRS 패턴 실습환경 정리를 시작합니다..."

    # 환경 변수 설정
    setup_environment "$1"

    # 순서대로 정리 진행
    cleanup_application
    cleanup_databases
    cleanup_storage
    cleanup_event_hub  # Event Hub 정리 추가
    cleanup_namespaces

    log "정리가 완료되었습니다."
    log "남은 리소스 확인:"
    kubectl get all -n $DB_NAMESPACE 2>/dev/null || true
    kubectl get all -n $APP_NAMESPACE 2>/dev/null || true
}
