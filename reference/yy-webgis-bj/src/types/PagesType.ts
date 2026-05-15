export interface RolePermission {
  id: string,
  permName: string,
  permKey: string,
  value?: string,
  label?: string
}

export interface RoleFormData {
  roleName: string,
  permissions: RolePermission[] | string[],
  roleDesc: string
}

export interface UserRole {
  id?: number | string,
  roleName: string,
  roleKey?: string,
  roleDesc?: string,
  permissions: string[] | RolePermission[]
}

export interface UserRecords {
  id: number,
  email?: null | string,
  enabled?: boolean | null,
  loginname: string | null,
  password: string,
  phone?: string | null,
  rolename: string | null,
  username?: string | null,
  roles: UserRole[]
}

export interface DataType {
  id: number,
  dataSource: string,
  dataSourceName: string,
}

export interface DataTable {
  dataTime: string,
  arrived: number,
  total: number,
  unarrived: number,
  status: string,
  dataSource?: string,
  insertTime?: string
}

export interface StationItem {
  value: string;
  label: string;
  children: boolean;
}
export interface Province {
  id: number,
  province: string,
  provinceName: string | null
}

export interface City {
  id: number,
  province?: string | null,
  provinceId: number,
  city: string
}

export interface Cnty {
  id: number,
  province?: string | null,
  provinceId: number,
  city?: string | null,
  cityId: number,
  cnty: string
}

export interface Station {
  id: number,
  provinceId: number,
  cityId: number,
  cntyId: number,
  station: string,
  stationName: string | null
}

export interface LogRecords {
  id: number,
  optioncontent: string,
  optiontime: string,
  optiontype: string,
  username: string
}

export interface ModelList {
  createTime: string,
  lat?: null | string,
  lon?: null | string,
  // model?: string,
  // modelId: string,
  id: number,
  modelName: string,
  author: string,
  // modelType: string,
  // modelVersion: string,
  // params?: null | string,
  // publisher: string,
  // validTime: string,
  status: number,
  latestUpdateTime: string,
  nextUpdateTime: string
}
