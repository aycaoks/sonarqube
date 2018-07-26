/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.db.alm;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import org.assertj.core.api.AbstractAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.alm.ALM.BITBUCKETCLOUD;
import static org.sonar.db.alm.ALM.GITHUB;

public class AlmProjectMappingsDaoTest {

  private static final String A_UUID = "abcde1234";
  private static final String ANOTHER_UUID = "xyz789";
  private static final String EMPTY_STRING = "";

  private static final String A_REPO = "my_repo";
  private static final String ANOTHER_REPO = "another_repo";

  private static final String A_GITHUB_SLUG = null;
  private static final String ANOTHER_GITHUB_SLUG = "example/foo";

  private static final String A_URL = "foo url";
  private static final String ANOTHER_URL = "bar url";

  private static final long DATE = 1_600_000_000_000L;
  private static final long DATE_LATER = 1_700_000_000_000L;

  private System2 system2 = mock(System2.class);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester dbTester = DbTester.create(system2);

  private DbSession dbSession = dbTester.getSession();
  private UuidFactory uuidFactory = mock(UuidFactory.class);
  private AlmProjectMappingsDao underTest = new AlmProjectMappingsDao(system2, uuidFactory);

  @Test
  public void insert_throws_NPE_if_alm_is_null() {
    expectAlmNPE();

    underTest.insertOrUpdate(dbSession, null, A_REPO, A_UUID, A_GITHUB_SLUG, A_URL);
  }

  @Test
  public void insert_throws_IAE_if_repo_id_is_null() {
    expectRepoIdNullOrEmptyIAE();

    underTest.insertOrUpdate(dbSession, GITHUB, null, A_UUID, A_GITHUB_SLUG, A_URL);
  }

  @Test
  public void insert_throws_IAE_if_repo_id_is_empty() {
    expectRepoIdNullOrEmptyIAE();

    underTest.insertOrUpdate(dbSession, GITHUB, EMPTY_STRING, A_UUID, A_GITHUB_SLUG, A_URL);
  }

  @Test
  public void insert_throws_IAE_if_project_uuid_is_null() {
    expectProjectUuidNullOrEmptyIAE();

    underTest.insertOrUpdate(dbSession, GITHUB, A_REPO, null, A_GITHUB_SLUG, A_URL);
  }

  @Test
  public void insert_throws_IAE_if_project_uuid_is_empty() {
    expectProjectUuidNullOrEmptyIAE();

    underTest.insertOrUpdate(dbSession, GITHUB, A_REPO, EMPTY_STRING, A_GITHUB_SLUG, A_URL);
  }

  @Test
  public void insert_throws_IAE_if_url_is_null() {
    expectUrlNullOrEmptyIAE();

    underTest.insertOrUpdate(dbSession, GITHUB, A_REPO, A_UUID, A_GITHUB_SLUG, null);
  }

  @Test
  public void insert_throws_IAE_if_url_is_empty() {
    expectUrlNullOrEmptyIAE();

    underTest.insertOrUpdate(dbSession, GITHUB, A_REPO, A_UUID, A_GITHUB_SLUG, EMPTY_STRING);
  }

  @Test
  public void insert() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    when(system2.now()).thenReturn(DATE);
    underTest.insertOrUpdate(dbSession, GITHUB, A_REPO, A_UUID, A_GITHUB_SLUG, A_URL);

    assertThatAlmProjectMapping(GITHUB, A_REPO)
      .hasProjectUuid(A_UUID)
      .hasGithubSlug(A_GITHUB_SLUG)
      .hasUrl(A_URL)
      .hasCreatedAt(DATE)
      .hasUpdatedAt(DATE);
  }

  @Test
  public void update() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    when(system2.now()).thenReturn(DATE);
    underTest.insertOrUpdate(dbSession, GITHUB, A_REPO, A_UUID, A_GITHUB_SLUG, A_URL);

    when(system2.now()).thenReturn(DATE_LATER);
    underTest.insertOrUpdate(dbSession, GITHUB, A_REPO, ANOTHER_UUID, ANOTHER_GITHUB_SLUG, ANOTHER_URL);

    assertThatAlmProjectMapping(GITHUB, A_REPO)
      .hasProjectUuid(ANOTHER_UUID)
      .hasGithubSlug(ANOTHER_GITHUB_SLUG)
      .hasUrl(ANOTHER_URL)
      .hasCreatedAt(DATE)
      .hasUpdatedAt(DATE_LATER);
  }

  @Test
  public void insert_multiple() {
    when(system2.now()).thenReturn(DATE);
    when(uuidFactory.create())
      .thenReturn(A_UUID)
      .thenReturn(ANOTHER_UUID);
    underTest.insertOrUpdate(dbSession, GITHUB, A_REPO, A_UUID, A_GITHUB_SLUG, A_URL);
    underTest.insertOrUpdate(dbSession, GITHUB, ANOTHER_REPO, ANOTHER_UUID, ANOTHER_GITHUB_SLUG, ANOTHER_URL);

    assertThatAlmProjectMapping(GITHUB, A_REPO)
      .hasProjectUuid(A_UUID)
      .hasGithubSlug(A_GITHUB_SLUG)
      .hasUrl(A_URL)
      .hasCreatedAt(DATE)
      .hasUpdatedAt(DATE);

    assertThatAlmProjectMapping(GITHUB, ANOTHER_REPO)
      .hasProjectUuid(ANOTHER_UUID)
      .hasGithubSlug(ANOTHER_GITHUB_SLUG)
      .hasUrl(ANOTHER_URL)
      .hasCreatedAt(DATE)
      .hasUpdatedAt(DATE);
  }

  @Test
  public void mappingExists_throws_NPE_when_alm_is_null() {
    expectAlmNPE();

    underTest.mappingExists(dbSession, null, A_REPO);
  }

  @Test
  public void mappingExists_throws_IAE_when_repo_id_is_null() {
    expectRepoIdNullOrEmptyIAE();

    underTest.mappingExists(dbSession, GITHUB, null);
  }

  @Test
  public void mappingExists_throws_IAE_when_repo_id_is_empty() {
    expectRepoIdNullOrEmptyIAE();

    underTest.mappingExists(dbSession, GITHUB, EMPTY_STRING);
  }

  @Test
  public void mappingExists_returns_false_when_entry_does_not_exist_in_DB() {
    assertThat(underTest.mappingExists(dbSession, GITHUB, A_REPO)).isFalse();
  }

  @Test
  public void mappingExists_returns_true_when_entry_exists() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    underTest.insertOrUpdate(dbSession, GITHUB, A_REPO, A_UUID, A_GITHUB_SLUG, A_URL);

    assertThat(underTest.mappingExists(dbSession, GITHUB, A_REPO)).isTrue();
  }

  @Test
  public void select_by_repo_ids() {
    when(system2.now()).thenReturn(DATE);
    when(uuidFactory.create())
      .thenReturn("uuid1")
      .thenReturn("uuid2")
      .thenReturn("uuid3");
    underTest.insertOrUpdate(dbSession, GITHUB, A_REPO, A_UUID, A_GITHUB_SLUG, A_URL);
    underTest.insertOrUpdate(dbSession, GITHUB, ANOTHER_REPO, ANOTHER_UUID, null, ANOTHER_URL);
    underTest.insertOrUpdate(dbSession, BITBUCKETCLOUD, ANOTHER_REPO, "foo", null, "http://foo");

    assertThat(underTest.selectByRepoIds(dbSession, GITHUB, Arrays.asList(A_REPO, ANOTHER_REPO, "foo")))
      .extracting(AlmProjectMappingDto::getUuid, AlmProjectMappingDto::getAlmId, AlmProjectMappingDto::getRepoId, AlmProjectMappingDto::getProjectUuid, AlmProjectMappingDto::getUrl, AlmProjectMappingDto::getGithubSlug)
      .containsExactlyInAnyOrder(
        tuple("uuid1", GITHUB.getId(), A_REPO, A_UUID, A_URL, A_GITHUB_SLUG),
        tuple("uuid2", GITHUB.getId(), ANOTHER_REPO, ANOTHER_UUID, ANOTHER_URL, null));
  }

  private void expectAlmNPE() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("alm can't be null");
  }

  private void expectRepoIdNullOrEmptyIAE() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("repoId can't be null nor empty");
  }

  private void expectProjectUuidNullOrEmptyIAE() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("projectUuid can't be null nor empty");
  }

  private void expectUrlNullOrEmptyIAE() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("url can't be null nor empty");
  }

  private AlmAppInstallAssert assertThatAlmProjectMapping(ALM alm, String repoId) {
    return new AlmAppInstallAssert(dbTester, dbSession, alm, repoId);
  }

  private static class AlmAppInstallAssert extends AbstractAssert<AlmAppInstallAssert, AlmProjectMapping> {

    private AlmAppInstallAssert(DbTester dbTester, DbSession dbSession, ALM alm, String repoId) {
      super(asAlmProjectMapping(dbTester, dbSession, alm, repoId), AlmAppInstallAssert.class);
    }

    private static AlmProjectMapping asAlmProjectMapping(DbTester dbTester, DbSession dbSession, ALM alm, String repoId) {
      List<Map<String, Object>> rows = dbTester.select(
        dbSession,
        "select" +
          " project_uuid as \"projectUuid\", github_slug as \"githubSlug\", url as \"url\", " +
          " created_at as \"createdAt\", updated_at as \"updatedAt\"" +
          " from alm_project_mappings" +
          " where alm_id='" + alm.getId() + "' and repo_id='" + repoId + "'");
      if (rows.isEmpty()) {
        return null;
      }
      if (rows.size() > 1) {
        throw new IllegalStateException("Unique index violation");
      }
      return new AlmProjectMapping(
        (String) rows.get(0).get("projectUuid"),
        (String) rows.get(0).get("githubSlug"),
        (String) rows.get(0).get("url"),
        (Long) rows.get(0).get("createdAt"),
        (Long) rows.get(0).get("updatedAt"));
    }

    public void doesNotExist() {
      isNull();
    }

    AlmAppInstallAssert hasProjectUuid(String expected) {
      isNotNull();

      if (!Objects.equals(actual.projectUuid, expected)) {
        failWithMessage("Expected ALM Project Mapping to have column PROJECT_UUID to be <%s> but was <%s>", expected, actual.projectUuid);
      }
      return this;
    }

    AlmAppInstallAssert hasGithubSlug(String expected) {
      isNotNull();

      if (!Objects.equals(actual.githubSlug, expected)) {
        failWithMessage("Expected ALM Project Mapping to have column GITHUB_SLUG to be <%s> but was <%s>", expected, actual.githubSlug);
      }
      return this;
    }

    AlmAppInstallAssert hasUrl(String expected) {
      isNotNull();

      if (!Objects.equals(actual.url, expected)) {
        failWithMessage("Expected ALM Project Mapping to have column URL to be <%s> but was <%s>", expected, actual.url);
      }
      return this;
    }

    AlmAppInstallAssert hasCreatedAt(long expected) {
      isNotNull();

      if (!Objects.equals(actual.createdAt, expected)) {
        failWithMessage("Expected ALM Project Mapping to have column CREATED_AT to be <%s> but was <%s>", expected, actual.createdAt);
      }

      return this;
    }

    AlmAppInstallAssert hasUpdatedAt(long expected) {
      isNotNull();

      if (!Objects.equals(actual.updatedAt, expected)) {
        failWithMessage("Expected ALM Project Mapping to have column UPDATED_AT to be <%s> but was <%s>", expected, actual.updatedAt);
      }

      return this;
    }

  }

  private static final class AlmProjectMapping {
    private final String projectUuid;
    private final String githubSlug;
    private final String url;
    private final Long createdAt;
    private final Long updatedAt;

    AlmProjectMapping(@Nullable String projectUuid, @Nullable String githubSlug, @Nullable String url, @Nullable Long createdAt, @Nullable Long updatedAt) {
      this.projectUuid = projectUuid;
      this.githubSlug = githubSlug;
      this.url = url;
      this.createdAt = createdAt;
      this.updatedAt = updatedAt;
    }
  }
}
