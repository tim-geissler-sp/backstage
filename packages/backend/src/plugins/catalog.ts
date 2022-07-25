import { CatalogBuilder } from '@backstage/plugin-catalog-backend';
import { ScaffolderEntitiesProcessor } from '@backstage/plugin-scaffolder-backend';
import { Router } from 'express';
import { PluginEnvironment } from '../types';
import { GithubOrgReaderProcessor } from '@backstage/plugin-catalog-backend-module-github';


export default async function createPlugin(
  env: PluginEnvironment,
): Promise<Router> {
  const builder = await CatalogBuilder.create(env);
  builder.addProcessor(new ScaffolderEntitiesProcessor());
  // Add processer to read users from GH ord
  builder.addProcessor(
    GithubOrgReaderProcessor.fromConfig(env.config, { logger: env.logger }),
  );
  
  const { processingEngine, router } = await builder.build();
  await processingEngine.start();
  return router;
}


