#!/usr/bin/env bash
set -euo pipefail
if [[ $# != 2 || ! $1 =~ ^(check|deploy)$ || ! $2 =~ ^[a-f0-9]{40}$ ]]; then
    echo "Usage: $0 check|deploy FULL_COMMIT_SHA" >&2
    exit 2
fi
# Fixed document and instance: no production target or arbitrary shell input.
instance=i-0eb11976c412eb88b
command_id=$(aws ssm send-command \
    --region ap-northeast-2 \
    --instance-ids "$instance" \
    --document-name AicsPrTestDeploy \
    --document-version 3 \
    --parameters "CommitSha=$2,Mode=$1" \
    --timeout-seconds 120 \
    --query Command.CommandId --output text --no-cli-pager)

deadline=$((SECONDS + 2520))
while (( SECONDS < deadline )); do
    if ! status=$(aws ssm get-command-invocation \
        --region ap-northeast-2 --command-id "$command_id" --instance-id "$instance" \
        --query Status --output text --no-cli-pager 2>/dev/null); then
        sleep 5
        continue
    fi
    case "$status" in
        Success) echo "Development command completed successfully."; exit 0 ;;
        Pending|InProgress|Delayed) sleep 5 ;;
        *) echo "Development command failed: $status (command $command_id)." >&2; exit 1 ;;
    esac
done
# Do not cancel a remote deployment or rollback because the observer timed out.
echo "Observer timed out; inspect command $command_id before starting another deployment." >&2
exit 1
