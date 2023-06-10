/*
 * Copyright 2019-2023 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.cloudnetservice.modules.rest.v2;

import eu.cloudnetservice.common.io.FileUtil;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.driver.module.ModuleProvider;
import eu.cloudnetservice.driver.module.ModuleWrapper;
import eu.cloudnetservice.driver.module.driver.DriverModule;
import eu.cloudnetservice.driver.network.http.HttpContext;
import eu.cloudnetservice.driver.network.http.HttpResponseCode;
import eu.cloudnetservice.driver.network.http.annotation.HttpRequestHandler;
import eu.cloudnetservice.driver.network.http.annotation.RequestBody;
import eu.cloudnetservice.driver.network.http.annotation.RequestPathParam;
import eu.cloudnetservice.node.config.Configuration;
import eu.cloudnetservice.node.http.V2HttpHandler;
import eu.cloudnetservice.node.http.annotation.BearerAuth;
import eu.cloudnetservice.node.http.annotation.HandlerScope;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.function.Consumer;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

@Singleton
public final class V2HttpHandlerModule extends V2HttpHandler {

  private final ModuleProvider moduleProvider;

  @Inject
  public V2HttpHandlerModule(@NonNull Configuration config, @NonNull ModuleProvider moduleProvider) {
    super(config.restConfiguration());
    this.moduleProvider = moduleProvider;
  }

  @BearerAuth
  @HandlerScope({"rest:module_write", "rest:module_reload_all"})
  @HttpRequestHandler(paths = "/api/v2/module/reload")
  private void handleReloadRequest(@NonNull HttpContext context) {
    this.moduleProvider.reloadAll();

    this.ok(context)
      .body(this.success().toString())
      .context()
      .closeAfter(true)
      .cancelNext(true);
  }

  @BearerAuth
  @HandlerScope({"rest:module_read", "rest:module_list"})
  @HttpRequestHandler(paths = "/api/v2/module")
  private void handleModuleListRequest(@NonNull HttpContext context) {
    this.ok(context)
      .body(this.success()
        .append("modules", this.moduleProvider.modules().stream()
          .map(module -> Document.newJsonDocument()
            .append("lifecycle", module.moduleLifeCycle())
            .append("configuration", module.moduleConfiguration()))
          .toList())
        .toString())
      .context()
      .closeAfter(true)
      .cancelNext(true);
  }

  @BearerAuth
  @HandlerScope({"rest:module_read", "rest:module_get"})
  @HttpRequestHandler(paths = "/api/v2/module/{module}")
  private void handleModuleRequest(@NonNull HttpContext context, @NonNull @RequestPathParam("module") String name) {
    this.handleWithModuleContext(context, name, module -> this.showModule(context, module));
  }

  @BearerAuth
  @HandlerScope({"rest:module_write", "rest:module_reload"})
  @HttpRequestHandler(paths = "/api/v2/module/{module}/reload")
  private void handleModuleReloadRequest(@NonNull HttpContext ctx, @NonNull @RequestPathParam("module") String name) {
    this.handleWithModuleContext(ctx, name, module -> {
      module.reloadModule();
      this.ok(ctx)
        .body(this.success().toString())
        .context()
        .closeAfter(true)
        .cancelNext(true);
    });
  }

  @BearerAuth
  @HandlerScope({"rest:module_write", "rest:module_unload"})
  @HttpRequestHandler(paths = "/api/v2/module/{module}/unload")
  private void handleModuleUnloadRequest(@NonNull HttpContext ctx, @NonNull @RequestPathParam("module") String name) {
    this.handleWithModuleContext(ctx, name, module -> {
      module.unloadModule();
      this.ok(ctx)
        .body(this.success().toString())
        .context()
        .closeAfter(true)
        .cancelNext(true);
    });
  }

  @BearerAuth
  @HandlerScope({"rest:module_write", "rest:module_load"})
  @HttpRequestHandler(paths = "/api/v2/module/{module}/load", methods = "PUT")
  private void handleModuleLoadRequest(
    @NonNull HttpContext context,
    @NonNull @RequestPathParam("module") String name,
    @NonNull @RequestBody InputStream body
  ) {
    var moduleTarget = this.moduleProvider.moduleDirectoryPath().resolve(name);
    FileUtil.ensureChild(this.moduleProvider.moduleDirectoryPath(), moduleTarget);

    try (var outputStream = Files.newOutputStream(moduleTarget)) {
      FileUtil.copy(body, outputStream);
    } catch (IOException exception) {
      this.response(context, HttpResponseCode.INTERNAL_SERVER_ERROR)
        .body(this.failure().append("reason", "Unable to copy module file").toString())
        .context()
        .closeAfter(true)
        .cancelNext(true);
      FileUtil.delete(moduleTarget);
      return;
    }

    this.showModule(context, this.moduleProvider.loadModule(moduleTarget));
  }

  @BearerAuth
  @HandlerScope({"rest:module_read", "rest:module_config_get"})
  @HttpRequestHandler(paths = "/api/v2/module/{module}/config")
  private void handleModuleConfigRequest(@NonNull HttpContext ctx, @NonNull @RequestPathParam("module") String name) {
    this.handleWithModuleContext(ctx, name, module -> {
      if (module.module() instanceof DriverModule driverModule) {
        var config = driverModule.readConfig(DocumentFactory.json());
        this.ok(ctx)
          .body(this.success().append("config", config).toString())
          .context()
          .closeAfter(true)
          .cancelNext(true);
      } else {
        this.ok(ctx)
          .body(this.failure().append("reason", "Module was not loaded by CloudNet").toString())
          .context()
          .closeAfter(true)
          .cancelNext(true);
      }
    });
  }

  @BearerAuth
  @HandlerScope({"rest:module_write", "rest:module_config_update"})
  @HttpRequestHandler(paths = "/api/v2/module/{module}/config", methods = "POST")
  private void handleConfigUpdateRequest(
    @NonNull HttpContext ctx,
    @NonNull @RequestPathParam("module") String name,
    @NonNull @RequestBody InputStream body
  ) {
    this.handleWithModuleContext(ctx, name, module -> {
      if (module.module() instanceof DriverModule driverModule) {
        driverModule.writeConfig(Document.newJsonDocument().appendTree(body));
        this.ok(ctx)
          .body(this.success().toString())
          .context()
          .closeAfter(true)
          .cancelNext(true);
      } else {
        this.ok(ctx)
          .body(this.failure().append("reason", "Module was not loaded by CloudNet").toString())
          .context()
          .closeAfter(true)
          .cancelNext(true);
      }
    });
  }

  private void showModule(@NonNull HttpContext context, @Nullable ModuleWrapper wrapper) {
    if (wrapper == null) {
      this.ok(context)
        .body(this.failure().toString())
        .context()
        .closeAfter(true)
        .cancelNext(true);
    } else {
      this.ok(context)
        .body(this.success()
          .append("lifecycle", wrapper.moduleLifeCycle())
          .append("configuration", wrapper.moduleConfiguration())
          .toString()
        ).context().closeAfter(true).cancelNext(true);
    }
  }

  private void handleWithModuleContext(
    @NonNull HttpContext context,
    @NonNull String name,
    @NonNull Consumer<ModuleWrapper> handler
  ) {
    var wrapper = this.moduleProvider.module(name);
    if (wrapper == null) {
      this.notFound(context)
        .body(this.failure().append("reason", "No such module").toString())
        .context()
        .closeAfter(true)
        .cancelNext(true);
      return;
    }

    handler.accept(wrapper);
  }
}
