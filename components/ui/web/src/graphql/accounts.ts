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

