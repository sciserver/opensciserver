function getKeys(obj) {
    return Object.keys(obj);
}
function runningJobs(obj) {
    const keys = getKeys(obj);
    let jobs = 0;
    for (let i = 0; i < keys.length; i += 1) {
      const key = keys[i];
      if (key < 32) {
        jobs += obj[keys[i]];
      }
    }
    return jobs;
  }
  function completedJobs(obj) {
    const keys = getKeys(obj);
    let jobs = 0;
    for (let i = 0; i < keys.length; i += 1) {
      const key = keys[i];
      if (key >= 32) {
        jobs += obj[keys[i]];
      }
    }
    return jobs;
  }
  export default { runningJobs, completedJobs, getKeys };
