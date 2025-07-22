/* eslint-disable import/no-extraneous-dependencies */
/* eslint-disable import/no-cycle */
import { RESTDataSource, AugmentedRequest } from '@apollo/datasource-rest';
import type { KeyValueCache } from '@apollo/utils.keyvaluecache';

import { environment } from '../environment';
import { User } from '../generated/typings';

export class AccountsAPI extends RESTDataSource {
  private loginPortalURL = `${environment.loginPortal.baseUrl}`
  private racmURL = `${environment.racm.userUrl}`;
  private token: string;

  constructor(options: { token: string; cache: KeyValueCache }) {
    super(options); // this sends our server's `cache` through
    this.token = options.token;
  }

  override willSendRequest(path: string, request: AugmentedRequest) {
    request.headers['X-Auth-Token'] = this.token;
  }

  // QUERIES //
  async getUser(): Promise<User> {
    const res = await this.get(`${this.racmURL!}user`);

    return this.userReducer(res);
  }

  // MUTATIONS //
  async login(username: string, password: string): Promise<string> {
    const token = await this.post(`${this.loginPortalURL!}auth`,
      {
        body:
        {
          username,
          password
        }
      }
    );

    return token;
  }

  // Reducers
  userReducer(res: any): User {
    return {
      id: res.id,
      userName: res.username,
      email: res.contactEmail,
      visibility: res.visibility
    };
  }

}
