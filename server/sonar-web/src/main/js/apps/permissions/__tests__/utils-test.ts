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
import { isSonarCloud } from '../../../helpers/system';
import { convertToPermissionDefinitions } from '../utils';

jest.mock('../../../helpers/system', () => ({ isSonarCloud: jest.fn() }));

jest.mock('../../../helpers/l10nBundle', () => ({
  getMessages: jest.fn().mockReturnValue({})
}));

describe('convertToPermissionDefinitions', () => {
  it('should convert and translate a permission definition', () => {
    (isSonarCloud as jest.Mock).mockImplementation(() => false);

    const data = convertToPermissionDefinitions(['admin'], 'global_permissions');
    const expected = [
      {
        description: 'global_permissions.admin.desc',
        key: 'admin',
        name: 'global_permissions.admin'
      }
    ];

    expect(data).toEqual(expected);
  });

  it('should convert and translate a permission definition for SonarCloud', () => {
    (isSonarCloud as jest.Mock).mockImplementation(() => true);

    const data = convertToPermissionDefinitions(['admin'], 'global_permissions');
    const expected = [
      {
        description: 'global_permissions.admin.desc.sonarcloud',
        key: 'admin',
        name: 'global_permissions.admin.sonarcloud'
      }
    ];

    expect(data).toEqual(expected);
  });

  it('should fallback to basic message when SonarCloud version does not exist', () => {
    (isSonarCloud as jest.Mock).mockImplementation(() => true);

    const data = convertToPermissionDefinitions(['admin'], 'global_permissions');
    const expected = [
      {
        description: 'global_permissions.admin.desc.sonarcloud',
        key: 'admin',
        name: 'global_permissions.admin.sonarcloud'
      }
    ];

    expect(data).toEqual(expected);
  });
});
