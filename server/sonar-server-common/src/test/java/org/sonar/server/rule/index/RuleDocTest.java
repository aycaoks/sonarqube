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
package org.sonar.server.rule.index;

import org.junit.Test;
import org.sonar.db.rule.RuleDescriptionSectionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleForIndexingDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.security.SecurityStandards;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.markdown.Markdown.convertToHtml;
import static org.sonar.server.security.SecurityStandards.fromSecurityStandards;

public class RuleDocTest {

  @Test
  public void ruleDocOf_mapsFieldCorrectly() {
    RuleDto ruleDto = RuleTesting.newRule();
    RuleForIndexingDto ruleForIndexingDto = RuleForIndexingDto.fromRuleDto(ruleDto);
    ruleForIndexingDto.setTemplateRuleKey("templateKey");
    ruleForIndexingDto.setTemplateRepository("repoKey");
    SecurityStandards securityStandards = fromSecurityStandards(ruleDto.getSecurityStandards());

    RuleDoc ruleDoc = RuleDoc.createFrom(ruleForIndexingDto, securityStandards);

    assertThat(ruleDoc.getId()).isEqualTo(ruleDto.getUuid());
    assertThat(ruleDoc.key()).isEqualTo(ruleForIndexingDto.getRuleKey());
    assertThat(ruleDoc.repository()).isEqualTo(ruleForIndexingDto.getRepository());
    assertThat(ruleDoc.internalKey()).isEqualTo(ruleForIndexingDto.getInternalKey());
    assertThat(ruleDoc.isExternal()).isEqualTo(ruleForIndexingDto.isExternal());
    assertThat(ruleDoc.language()).isEqualTo(ruleForIndexingDto.getLanguage());
    assertThat(ruleDoc.getCwe()).isEqualTo(securityStandards.getCwe());
    assertThat(ruleDoc.getOwaspTop10()).isEqualTo(securityStandards.getOwaspTop10());
    assertThat(ruleDoc.getOwaspTop10For2021()).isEqualTo(securityStandards.getOwaspTop10For2021());
    assertThat(ruleDoc.getSansTop25()).isEqualTo(securityStandards.getSansTop25());
    assertThat(ruleDoc.getSonarSourceSecurityCategory()).isEqualTo(securityStandards.getSqCategory());
    assertThat(ruleDoc.name()).isEqualTo(ruleForIndexingDto.getName());
    assertThat(ruleDoc.ruleKey()).isEqualTo(ruleForIndexingDto.getPluginRuleKey());
    assertThat(ruleDoc.severity()).isEqualTo(ruleForIndexingDto.getSeverityAsString());
    assertThat(ruleDoc.status()).isEqualTo(ruleForIndexingDto.getStatus());
    assertThat(ruleDoc.type().name()).isEqualTo(ruleForIndexingDto.getTypeAsRuleType().name());
    assertThat(ruleDoc.createdAt()).isEqualTo(ruleForIndexingDto.getCreatedAt());
    assertThat(ruleDoc.getTags()).isEqualTo(ruleForIndexingDto.getSystemTags());
    assertThat(ruleDoc.updatedAt()).isEqualTo(ruleForIndexingDto.getUpdatedAt());
    assertThat(ruleDoc.templateKey().repository()).isEqualTo(ruleForIndexingDto.getTemplateRepository());
    assertThat(ruleDoc.templateKey().rule()).isEqualTo(ruleForIndexingDto.getTemplateRuleKey());

  }

  @Test
  public void ruleDocOf_whenGivenNoHtmlSections_hasEmptyStringInHtmlDescription() {
    RuleDto ruleDto = RuleTesting.newRule();
    ruleDto.setDescriptionFormat(RuleDto.Format.HTML);
    ruleDto.getRuleDescriptionSectionDtos().clear();

    RuleForIndexingDto ruleForIndexingDto = RuleForIndexingDto.fromRuleDto(ruleDto);
    SecurityStandards securityStandards = fromSecurityStandards(ruleDto.getSecurityStandards());

    RuleDoc ruleDoc = RuleDoc.createFrom(ruleForIndexingDto, securityStandards);
    assertThat(ruleDoc.htmlDescription()).isEmpty();
  }

  @Test
  public void ruleDocOf_whenGivenMultipleHtmlSections_hasConcatenationInHtmlDescription() {
    RuleDto ruleDto = RuleTesting.newRule();
    ruleDto.setDescriptionFormat(RuleDto.Format.HTML);
    ruleDto.getRuleDescriptionSectionDtos().clear();
    RuleDescriptionSectionDto section1 = buildRuleDescriptionSectionDto("section1", "<p>html content 1</p>");
    RuleDescriptionSectionDto section2 = buildRuleDescriptionSectionDto("section2", "<p>html content 2</p>");
    ruleDto.addRuleDescriptionSectionDto(section1);
    ruleDto.addRuleDescriptionSectionDto(section2);

    RuleForIndexingDto ruleForIndexingDto = RuleForIndexingDto.fromRuleDto(ruleDto);
    SecurityStandards securityStandards = fromSecurityStandards(ruleDto.getSecurityStandards());

    RuleDoc ruleDoc = RuleDoc.createFrom(ruleForIndexingDto, securityStandards);
    assertThat(ruleDoc.htmlDescription())
      .contains(section1.getContent())
      .contains(section2.getContent())
      .hasSameSizeAs(section1.getContent() + " " + section2.getContent());
  }

  @Test
  public void ruleDocOf_whenGivenMultipleMarkdownSections_transformToHtmlAndConcatenatesInHtmlDescription() {
    RuleDto ruleDto = RuleTesting.newRule();
    ruleDto.setDescriptionFormat(RuleDto.Format.MARKDOWN);
    ruleDto.getRuleDescriptionSectionDtos().clear();
    RuleDescriptionSectionDto section1 = buildRuleDescriptionSectionDto("section1", "*html content 1*");
    RuleDescriptionSectionDto section2 = buildRuleDescriptionSectionDto("section2", "*html content 2*");
    ruleDto.addRuleDescriptionSectionDto(section1);
    ruleDto.addRuleDescriptionSectionDto(section2);

    RuleForIndexingDto ruleForIndexingDto = RuleForIndexingDto.fromRuleDto(ruleDto);
    SecurityStandards securityStandards = fromSecurityStandards(ruleDto.getSecurityStandards());

    RuleDoc ruleDoc = RuleDoc.createFrom(ruleForIndexingDto, securityStandards);
    assertThat(ruleDoc.htmlDescription())
      .contains(convertToHtml(section1.getContent()))
      .contains(convertToHtml(section2.getContent()))
      .hasSameSizeAs(convertToHtml(section1.getContent()) + " " + convertToHtml(section2.getContent()));
  }

  private static RuleDescriptionSectionDto buildRuleDescriptionSectionDto(String key, String content) {
    return RuleDescriptionSectionDto.builder().key(key).content(content).build();
  }
}
