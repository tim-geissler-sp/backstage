ARG BASE_IMAGE=golang:1.16-alpine3.13
ARG RUNTIME_IMAGE=alpine:3.13

FROM $BASE_IMAGE AS base
WORKDIR /app
RUN GOBIN=/app/bin GOOS=linux go install github.com/kisielk/errcheck@v1.6.0

COPY . .
RUN apk --no-cache add build-base pkgconfig
RUN GOOS=linux /app/bin/errcheck ./...

# builder image
FROM base AS test
ENV GOOS=linux
CMD go test -tags "musl integration" ./...

# build image
FROM test AS builder
RUN GOOS=linux go build -tags musl -o app ./cmd/sp-scheduler

# runtime image
FROM $RUNTIME_IMAGE AS runtime
# tzdata package is such that Go can leverage timezone data correctly
RUN apk update && apk --no-cache add ca-certificates tzdata

WORKDIR /app
COPY --from=builder /app/app .
COPY --from=builder /app/migrations migrations
EXPOSE 7100
EXPOSE 7200

# default environment
ENV ATLAS_PRODUCTION true

CMD ["./app"]
