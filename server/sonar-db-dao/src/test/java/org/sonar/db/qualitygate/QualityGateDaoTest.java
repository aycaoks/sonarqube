/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.db.qualitygate;

import java.util.Collections;
import java.util.Date;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.project.ProjectDto;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class QualityGateDaoTest {

  @Rule
  public final DbTester db = DbTester.create(System2.INSTANCE);

  private final QualityGateDbTester qualityGateDbTester = new QualityGateDbTester(db);
  private final DbSession dbSession = db.getSession();
  private final QualityGateDao underTest = db.getDbClient().qualityGateDao();

  @Test
  public void insert() {
    QualityGateDto newQgate = new QualityGateDto()
      .setUuid(Uuids.createFast())
      .setName("My Quality Gate")
      .setBuiltIn(false)
      .setUpdatedAt(new Date());

    underTest.insert(dbSession, newQgate);
    dbSession.commit();

    QualityGateDto reloaded = underTest.selectByUuid(dbSession, newQgate.getUuid());
    assertThat(reloaded.getName()).isEqualTo("My Quality Gate");
    assertThat(reloaded.getUuid()).isEqualTo(newQgate.getUuid());
    assertThat(reloaded.isBuiltIn()).isFalse();
    assertThat(reloaded.getCreatedAt()).isNotNull();
    assertThat(reloaded.getUpdatedAt()).isNotNull();
  }

  @Test
  public void insert_built_in() {
    underTest.insert(db.getSession(), new QualityGateDto().setName("test").setBuiltIn(true).setUuid(Uuids.createFast()));

    QualityGateDto reloaded = underTest.selectByName(db.getSession(), "test");

    assertThat(reloaded.isBuiltIn()).isTrue();
  }

  @Test
  public void select_all() {
    QualityGateDto qualityGate1 = qualityGateDbTester.insertQualityGate();
    QualityGateDto qualityGate2 = qualityGateDbTester.insertQualityGate();
    QualityGateDto qualityGateOnOtherOrg = qualityGateDbTester.insertQualityGate();

    assertThat(underTest.selectAll(dbSession))
      .extracting(QualityGateDto::getUuid)
      .containsExactlyInAnyOrder(qualityGate1.getUuid(), qualityGate2.getUuid(), qualityGateOnOtherOrg.getUuid());
  }

  @Test
  public void testSelectByName() {
    insertQualityGates();
    assertThat(underTest.selectByName(dbSession, "Balanced").getName()).isEqualTo("Balanced");
    assertThat(underTest.selectByName(dbSession, "Unknown")).isNull();
  }

  @Test
  public void testSelectById() {
    insertQualityGates();
    assertThat(underTest.selectByUuid(dbSession, underTest.selectByName(dbSession, "Very strict").getUuid()).getName()).isEqualTo("Very strict");
    assertThat(underTest.selectByUuid(dbSession, "-1")).isNull();
  }

  @Test
  public void testSelectByUuid() {
    insertQualityGates();
    assertThat(underTest.selectByUuid(dbSession, underTest.selectByName(dbSession, "Very strict").getUuid()).getName()).isEqualTo("Very strict");
    assertThat(underTest.selectByUuid(dbSession, "not-existing-uuid")).isNull();
  }

  @Test
  public void select_by_project_uuid() {
    ProjectDto project = db.components().insertPrivateProjectDto();

    QualityGateDto qualityGate1 = db.qualityGates().insertQualityGate();
    QualityGateDto qualityGate2 = db.qualityGates().insertQualityGate();

    QualityGateDto qualityGate3 = db.qualityGates().insertQualityGate();

    db.qualityGates().associateProjectToQualityGate(project, qualityGate1);

    assertThat(underTest.selectByProjectUuid(dbSession, project.getUuid()).getUuid()).isEqualTo(qualityGate1.getUuid());
    assertThat(underTest.selectByProjectUuid(dbSession, "not-existing-uuid")).isNull();
  }

  @Test
  public void delete() {
    QualityGateDto qualityGate = qualityGateDbTester.insertQualityGate();
    QualityGateDto otherQualityGate = qualityGateDbTester.insertQualityGate();

    underTest.delete(qualityGate, dbSession);
    dbSession.commit();

    assertThat(underTest.selectByUuid(dbSession, qualityGate.getUuid())).isNull();
    assertThat(underTest.selectByUuid(dbSession, otherQualityGate.getUuid())).isNotNull();
  }

  @Test
  public void delete_by_uuids() {
    QualityGateDto qualityGate1 = qualityGateDbTester.insertQualityGate();
    QualityGateDto qualityGate2 = qualityGateDbTester.insertQualityGate();

    underTest.deleteByUuids(dbSession, asList(qualityGate1.getUuid(), qualityGate2.getUuid()));
    dbSession.commit();

    assertThat(underTest.selectAll(dbSession).stream())
      .extracting(QualityGateDto::getUuid)
      .doesNotContain(qualityGate1.getUuid(), qualityGate2.getUuid());
  }

  @Test
  public void delete_by_uuids_does_nothing_on_empty_list() {
    int nbOfQualityGates = db.countRowsOfTable(dbSession, "quality_gates");
    underTest.deleteByUuids(dbSession, Collections.emptyList());
    dbSession.commit();

    assertThat(db.countRowsOfTable(dbSession, "quality_gates")).isEqualTo(nbOfQualityGates);
  }

  @Test
  public void update() {
    QualityGateDto qualityGate = qualityGateDbTester.insertQualityGate(qg -> qg.setName("old name"));

    underTest.update(qualityGate.setName("Not so strict"), dbSession);
    dbSession.commit();

    QualityGateDto reloaded = underTest.selectByUuid(dbSession, qualityGate.getUuid());
    assertThat(reloaded.getName()).isEqualTo("Not so strict");
  }

  @Test
  public void selectBuiltIn() {
    QualityGateDto builtInQualityGate = qualityGateDbTester.insertQualityGate(qg -> qg.setName("Built in").setBuiltIn(true));
    QualityGateDto qualityGate = qualityGateDbTester.insertQualityGate(qg -> qg.setName("Random quality gate").setBuiltIn(false));
    dbSession.commit();

    QualityGateDto result = underTest.selectBuiltIn(dbSession);

    assertThat(result.getUuid()).isEqualTo(builtInQualityGate.getUuid());
    assertThat(result.getName()).isEqualTo(builtInQualityGate.getName());
  }

  @Test
  public void ensureOneBuiltInQualityGate() {
    String builtInQgName = "Sonar Way";
    QualityGateDto builtInQualityGate = qualityGateDbTester.insertQualityGate(qg -> qg.setName(builtInQgName).setBuiltIn(true));
    QualityGateDto qualityGate1 = qualityGateDbTester.insertQualityGate(qg -> qg.setName("QG1").setBuiltIn(true));
    QualityGateDto qualityGate2 = qualityGateDbTester.insertQualityGate(qg -> qg.setName("QG2"));

    underTest.ensureOneBuiltInQualityGate(dbSession, builtInQgName);
    dbSession.commit();

    QualityGateDto reloaded = underTest.selectByName(dbSession, builtInQgName);
    assertThat(reloaded.getUuid()).isEqualTo(builtInQualityGate.getUuid());
    assertThat(reloaded.getName()).isEqualTo(builtInQualityGate.getName());
    assertThat(reloaded.isBuiltIn()).isTrue();
  }

  private void insertQualityGates() {
    qualityGateDbTester.insertQualityGate(g -> g.setName("Very strict").setBuiltIn(false));
    qualityGateDbTester.insertQualityGate(g -> g.setName("Balanced").setBuiltIn(false));
    qualityGateDbTester.insertQualityGate(g -> g.setName("Lenient").setBuiltIn(false));
  }
}
