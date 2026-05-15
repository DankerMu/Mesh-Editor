
/**
 *
 * @param {string} roles
 * @returns
 * @example
 * hasPermission(['read'])
 */
export const hasPermission = (role) => {
  const userInfo = sessionStorage.getItem("userInfo")
  if(userInfo) {
    const rolesobj = JSON.parse(userInfo).roles
    const rolesArr = rolesobj[0].permissions.map(item=>item.permKey)
    return rolesArr.includes(role)
  } else return false
}
