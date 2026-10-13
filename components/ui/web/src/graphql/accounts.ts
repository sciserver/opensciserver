import { gql } from '@apollo/client';

export const LOGIN = gql`
  mutation login($username: String!, $password: String!) {
    login(username: $username, password: $password)
  }  
`;

export const GET_USER = gql`
  query GetUser {
    getUser {
      id
      userName
      email
      visibility
    }
  } 
`;


/** Every user and group a volume can be shared with. Large, so only loaded when the sharing dialog opens. */
export const PUBLIC_USERS_AND_GROUPS = gql`
  query publicUsersAndGroups {
    getPublicUsersAndGroups {
      id
      name
      type
    }
  }
`;
