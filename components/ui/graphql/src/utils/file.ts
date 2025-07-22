export const convertBase64toJSON = (content: string) => {
  const byteCharacters = Buffer.from(content, 'base64');
  const requestBodyObject = JSON.parse(byteCharacters.toString());
  return requestBodyObject;
};