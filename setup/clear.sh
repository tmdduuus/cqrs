#!/bin/bash

# ===========================================
# CQRS Pattern 실습환경 정리 스크립트
# ===========================================

# 사용법 출력
print_usage() {
    cat << EOF
사용법:
    $0 <userid>

설명:
    CQRS 패턴 실습을 위해 생성한 리소스를 정리합니다.
    Event Hub는 삭제하지 않습니다.

예제:
    $0 gappa     # gappa-cqrs 관련 리소스 정리
EOF
}

# 유틸리티 함수
log() {
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    echo "[$timestamp] $1"
}

# 환경 변수 설정
setup_environment() {
    USERID=$1
    NAME="${USERID}-cqrs"
    RESOURCE_GROUP="tiu-dgga-rg"
    DB_NAMESPACE="${USERID}-database"
    APP_NAMESPACE="${USERID}-application"
}

# 데이터베이스 정리
cleanup_databases() {
    log "데이터베이스 리소스 정리 중..."

    # StatefulSet 삭제
    log "StatefulSet 삭제 중..."
    kubectl delete statefulset -n $DB_NAMESPACE $NAME-postgres 2>/dev/null || true
    kubectl delete statefulset -n $DB_NAMESPACE $NAME-mongodb 2>/dev/null || true

    # ConfigMap 삭제
    log "ConfigMap 삭제 중..."
    kubectl delete configmap -n $DB_NAMESPACE postgres-init-script 2>/dev/null || true
    kubectl delete configmap -n $DB_NAMESPACE mongo-init-script 2>/dev/null || true

    # PVC 삭제
    log "PVC 삭제 중..."
    kubectl delete pvc -n $DB_NAMESPACE -l "app=postgres,userid=$USERID" 2>/dev/null || true
    kubectl delete pvc -n $DB_NAMESPACE -l "app=mongodb,userid=$USERID" 2>/dev/null || true

    # Service 삭제
    log "Service 삭제 중..."
    kubectl delete service -n $DB_NAMESPACE $NAME-postgres 2>/dev/null || true
    kubectl delete service -n $DB_NAMESPACE $NAME-mongodb 2>/dev/null || true

    log "데이터베이스 리소스 정리 완료"
}

# 애플리케이션 정리
cleanup_application() {
    log "애플리케이션 리소스 정리 중..."

    # ConfigMap 삭제
    log "ConfigMap 삭제 중..."
    kubectl delete cm $NAME-config -n $APP_NAMESPACE 2>/dev/null || true

    # Secret 삭제
    log "Secret 삭제 중..."
    kubectl delete secret storage-secret -n $APP_NAMESPACE 2>/dev/null || true
    kubectl delete secret eventhub-secret -n $APP_NAMESPACE 2>/dev/null || true

    # Deployment 삭제
    log "Deployment 삭제 중..."
    kubectl delete deployment -n $APP_NAMESPACE $NAME-command 2>/dev/null || true
    kubectl delete deployment -n $APP_NAMESPACE $NAME-query 2>/dev/null || true

    # Service 삭제
    log "Service 삭제 중..."
    kubectl delete service -n $APP_NAMESPACE $NAME-command 2>/dev/null || true
    kubectl delete service -n $APP_NAMESPACE $NAME-query 2>/dev/null || true

    log "애플리케이션 리소스 정리 완료"
}

# Storage 정리 (Event Hub 체크포인트 컨테이너만 삭제)
cleanup_storage() {
    log "Blob Storage 체크포인트 컨테이너 정리 중..."

    # Storage Account의 연결 문자열 가져오기
    STORAGE_CONNECTION_STRING=$(az storage account show-connection-string \
        --name dggastorage \
        --resource-group $RESOURCE_GROUP \
        --query connectionString \
        --output tsv)

    # Blob Container 삭제
    az storage container delete \
        --name eventhub-checkpoints \
        --connection-string "$STORAGE_CONNECTION_STRING" \
        --if-exists \
        2>/dev/null || true

    log "Blob Storage 정리 완료"
}

# Namespace 정리
cleanup_namespaces() {
    log "Namespace 정리 중..."

    # 각 Namespace의 모든 리소스가 삭제되었는지 확인 후 Namespace 삭제
    if ! kubectl get all -n $DB_NAMESPACE 2>/dev/null | grep -q .; then
        kubectl delete namespace $DB_NAMESPACE 2>/dev/null || true
        log "데이터베이스 Namespace 삭제 완료"
    else
        log "경고: 데이터베이스 Namespace에 아직 리소스가 있어 삭제하지 않습니다"
    fi

    if ! kubectl get all -n $APP_NAMESPACE 2>/dev/null | grep -q .; then
        kubectl delete namespace $APP_NAMESPACE 2>/dev/null || true
        log "애플리케이션 Namespace 삭제 완료"
    else
        log "경고: 애플리케이션 Namespace에 아직 리소스가 있어 삭제하지 않습니다"
    fi
}

# 메인 실행 함수
main() {
    log "CQRS 패턴 실습환경 정리를 시작합니다..."

    # 환경 변수 설정
    setup_environment "$1"

    # 순서대로 정리 진행
    cleanup_application
    cleanup_databases
    cleanup_storage
    cleanup_namespaces

    log "정리가 완료되었습니다."
    log "남은 리소스 확인:"
    kubectl get all -n $DB_NAMESPACE 2>/dev/null || true
    kubectl get all -n $APP_NAMESPACE 2>/dev/null || true
}

# 매개변수 검사
if [ $# -ne 1 ]; then
    print_usage
    exit 1
fi

# userid 유효성 검사
if [[ ! $1 =~ ^[a-z0-9]+$ ]]; then
    echo "Error: userid는 영문 소문자와 숫자만 사용할 수 있습니다."
    exit 1
fi

# 실행
main "$1"
