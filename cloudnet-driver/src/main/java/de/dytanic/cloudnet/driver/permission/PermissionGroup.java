/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

package de.dytanic.cloudnet.driver.permission;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * This interfaces provides access to the properties of a permission group
 */
public class PermissionGroup extends AbstractPermissible {

  public static final Type COL_GROUPS = TypeToken.getParameterized(Collection.class, PermissionGroup.class).getType();

  protected String color = "&7";
  protected String prefix = "&7";
  protected String suffix = "&f";
  protected String display = "&7";

  protected int sortId = 0;
  protected boolean defaultGroup = false;

  protected Collection<String> groups = new ArrayList<>();

  public PermissionGroup(@NotNull String name, int potency) {
    this.name = name;
    this.potency = potency;
  }

  public PermissionGroup(
    @NotNull String color,
    @NotNull String prefix,
    @NotNull String suffix,
    @NotNull String display,
    int sortId,
    boolean defaultGroup,
    @NotNull Collection<String> groups,
    @NotNull String name,
    int potency,
    long createdTime,
    @NotNull List<Permission> permissions,
    @NotNull Map<String, Collection<Permission>> groupPermissions,
    @NotNull JsonDocument properties
  ) {
    super(name, potency, createdTime, permissions, groupPermissions, properties);
    this.color = color;
    this.prefix = prefix;
    this.suffix = suffix;
    this.display = display;
    this.sortId = sortId;
    this.defaultGroup = defaultGroup;
    this.groups = groups;
  }

  public @NotNull Collection<String> getGroups() {
    return this.groups;
  }

  public void setGroups(@NotNull Collection<String> groups) {
    this.groups = groups;
  }

  public @NotNull String getPrefix() {
    return this.prefix;
  }

  public void setPrefix(@NotNull String prefix) {
    this.prefix = prefix;
  }

  public @NotNull String getColor() {
    return this.color;
  }

  public void setColor(@NotNull String color) {
    this.color = color;
  }

  public @NotNull String getSuffix() {
    return this.suffix;
  }

  public void setSuffix(@NotNull String suffix) {
    this.suffix = suffix;
  }

  public @NotNull String getDisplay() {
    return this.display;
  }

  public void setDisplay(@NotNull String display) {
    this.display = display;
  }

  public int getSortId() {
    return this.sortId;
  }

  public void setSortId(int sortId) {
    this.sortId = sortId;
  }

  public boolean isDefaultGroup() {
    return this.defaultGroup;
  }

  public void setDefaultGroup(boolean defaultGroup) {
    this.defaultGroup = defaultGroup;
  }

  @Override
  public @NotNull Collection<String> getGroupNames() {
    return this.getGroups();
  }
}
