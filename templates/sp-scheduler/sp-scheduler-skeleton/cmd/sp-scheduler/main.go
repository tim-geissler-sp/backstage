package main

import (
	"github.com/sailpoint/atlas-go/atlas/log"
	"github.com/sailpoint/sp-scheduler/internal/sp/scheduler/infra"

	_ "github.com/golang-migrate/migrate/v4/source/file"
)

func main() {
	service, err := infra.NewSchedulerService()
	if err != nil {
		log.Global().Sugar().Fatalf("init: %v", err)
	}
	defer service.Close()

	if err := service.Run(); err != nil {
		log.Global().Sugar().Fatalf("run: %v", err)
	}
}
