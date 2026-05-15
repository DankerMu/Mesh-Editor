// 图片基础路径
const IMAGE_BASE_PATH = './images/';

// 显示/隐藏内容板块
function showTab(tabId, event) {
  // 隐藏所有内容
  const contents = document.getElementsByClassName('content');
  for (let i = 0; i < contents.length; i++) {
    contents[i].classList.remove('active');
  }
  // 取消所有按钮的active状态
  const buttons = document.getElementsByClassName('nav-btn');
  for (let i = 0; i < buttons.length; i++) {
    buttons[i].classList.remove('active');
  }
  // 显示选中的内容并激活对应的按钮
  document.getElementById(tabId).classList.add('active');
  event.currentTarget.classList.add('active');

  // 切换到新标签时更新图片
  if (tabId === 'circulation') {
    updateCirculationImage();
  } else if (tabId === 'precipitation') {
    updatePrecipitationImage();
  } else if (tabId === 'element') {
    updateElementImage();
  }
  // 不需要为 profile 自动更新（需用户选择站点）
}

// 初始化选项按钮点击事件
function initOptionButtons(module) {
    // 高度层选项 (仅环流形势模块)
    if (module === 'circulation') {
        const levelOptions = document.querySelectorAll(`#${module}-level-options .option-btn`);
        // 默认选中500hPa
        levelOptions.forEach(btn => {
            if(btn.dataset.value === '500') {
                btn.classList.add('active');
            }
            btn.addEventListener('click', function() {
                levelOptions.forEach(b => b.classList.remove('active'));
                this.classList.add('active');
                updateCirculationImage();
            });
        });
    }
    
    // 产品类型选项
    const productOptions = document.querySelectorAll(`#${module}-product-options .option-btn`);
    productOptions.forEach(btn => {
        btn.addEventListener('click', function() {
            productOptions.forEach(b => b.classList.remove('active'));
            this.classList.add('active');
            if (module === 'circulation') {
                updateCirculationImage();
            } else if (module === 'precipitation') {
                updatePrecipitationPeriods();
                updatePrecipitationImage();
            } else if (module === 'element') {
                updateElementPeriods();
                updateElementImage();
            }
        });
    });
    
    // 区域选项
    const regionOptions = document.querySelectorAll(`#${module}-region-options .option-btn`);
    regionOptions.forEach(btn => {
        btn.addEventListener('click', function() {
            regionOptions.forEach(b => b.classList.remove('active'));
            this.classList.add('active');
            if (module === 'circulation') {
                updateCirculationImage();
            } else if (module === 'precipitation') {
                updatePrecipitationImage();
            } else if (module === 'element') {
                updateElementImage();
            }
        });
    });
}

// 初始化环流形势预报时效按钮
function initCirculationPeriods() {
    const container = document.getElementById('circulation-period-list');
    container.innerHTML = '';
    
    for (let i = 0; i <= 240; i += 6) {
        const btn = document.createElement('button');
        btn.className = 'option-btn';
        btn.textContent = i.toString().padStart(3, '0');
        btn.dataset.value = i;
        
        // 默认选中0小时预报时效
        if(i === 0) {
            btn.classList.add('active');
        }
        
        btn.addEventListener('click', function() {
            document.querySelectorAll('#circulation-period-list .option-btn').forEach(b => b.classList.remove('active'));
            this.classList.add('active');
            updateCirculationImage();
        });
        
        container.appendChild(btn);
    }
    
    // 确保至少有一个时效被选中
    if(!container.querySelector('.option-btn.active') && container.firstChild) {
        container.firstChild.classList.add('active');
    }
}

// 初始化降水量预报时效按钮
function initPrecipitationPeriods() {
    updatePrecipitationPeriods();
}

// 初始化要素预报时效按钮
function initElementPeriods() {
    updateElementPeriods();
}

// 更新降水量预报时效列表
function updatePrecipitationPeriods() {
    const container = document.getElementById('precipitation-period-list');
    container.innerHTML = '';
    
    const product = document.querySelector('#precipitation-product-options .option-btn.active')?.dataset.value || 'rain24';
    let start = 24, step = 12;
    
    switch(product) {
        case 'rain24': start = 24; break;
        case 'rain48': start = 48; break;
        case 'rain72': start = 72; break;
        case 'rain96': start = 96; break;
        case 'rain120': start = 120; break;
        case 'rain144': start = 144; break;
        case 'rain168': start = 168; break;
    }
    
    for (let i = start; i <= 240; i += step) {
        const btn = document.createElement('button');
        btn.className = 'option-btn';
        btn.textContent = i.toString().padStart(3, '0');
        btn.dataset.value = i;
        
        // 默认选中第一个预报时效
        if(i === start) {
            btn.classList.add('active');
        }
        
        btn.addEventListener('click', function() {
            document.querySelectorAll('#precipitation-period-list .option-btn').forEach(b => b.classList.remove('active'));
            this.classList.add('active');
            updatePrecipitationImage();
        });
        
        container.appendChild(btn);
    }
    
    // 确保至少有一个时效被选中
    if(!container.querySelector('.option-btn.active') && container.firstChild) {
        container.firstChild.classList.add('active');
    }
}

// 更新要素预报时效列表
function updateElementPeriods() {
    const container = document.getElementById('element-period-list');
    container.innerHTML = '';
    
    const product = document.querySelector('#element-product-options .option-btn.active')?.dataset.value || 'CAPE';
    let start = 0, step = 6;
    
    // 特殊处理24小时变温产品
    if(product === 'TMP_CHANGE_24H') {
        start = 24;
    }
    
    for (let i = start; i <= 240; i += step) {
        const btn = document.createElement('button');
        btn.className = 'option-btn';
        btn.textContent = i.toString().padStart(3, '0');
        btn.dataset.value = i;
        
        // 默认选中第一个预报时效
        if(i === start) {
            btn.classList.add('active');
        }
        
        btn.addEventListener('click', function() {
            document.querySelectorAll('#element-period-list .option-btn').forEach(b => b.classList.remove('active'));
            this.classList.add('active');
            updateElementImage();
        });
        
        container.appendChild(btn);
    }
    
    // 确保至少有一个时效被选中
    if(!container.querySelector('.option-btn.active') && container.firstChild) {
        container.firstChild.classList.add('active');
    }
}

// 滚动预报时效列表
function scrollPeriods(module, direction) {
    const container = document.querySelector(`#${module}-period-list`);
    const activeBtn = container.querySelector('.option-btn.active');
    const buttons = container.querySelectorAll('.option-btn');
    let currentIndex = Array.from(buttons).indexOf(activeBtn);
    
    if (direction === 1 && currentIndex < buttons.length - 1) {
        buttons[currentIndex].classList.remove('active');
        buttons[currentIndex + 1].classList.add('active');
        buttons[currentIndex + 1].scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        if (module === 'circulation') updateCirculationImage();
        else if (module === 'precipitation') updatePrecipitationImage();
        else if (module === 'element') updateElementImage();
    } else if (direction === -1 && currentIndex > 0) {
        buttons[currentIndex].classList.remove('active');
        buttons[currentIndex - 1].classList.add('active');
        buttons[currentIndex - 1].scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        if (module === 'circulation') updateCirculationImage();
        else if (module === 'precipitation') updatePrecipitationImage();
        else if (module === 'element') updateElementImage();
    }
}

// 更新环流形势图片
function updateCirculationImage() {
    const date = document.getElementById('circulation-date').value;
    const time = document.getElementById('circulation-time').value;
    const levelBtn = document.querySelector('#circulation-level-options .option-btn.active');
    const regionBtn = document.querySelector('#circulation-region-options .option-btn.active');
    const periodBtn = document.querySelector('#circulation-period-list .option-btn.active');
    
    const placeholder = document.getElementById('circulation-placeholder');
    
    // 检查所有必要参数是否已选择
    if (!date || !time || !levelBtn || !regionBtn || !periodBtn) {
        placeholder.textContent = '请选择所有必要参数';
        placeholder.style.display = 'block';
        return;
    }
    
    const level = levelBtn.dataset.value;
    const region = regionBtn.dataset.value;
    const period = periodBtn.dataset.value.padStart(3, '0');
    
    // 构造图片文件名和路径（根据新目录结构调整）
    const dateTime = date.replace(/-/g, '') + time;
    const filename = `circulation_${region}_${level}hPa_${dateTime}_${period}.png`;
    const imagePath = `${IMAGE_BASE_PATH}${dateTime}/circulation/${region}/${filename}`;
    
    // 创建或获取图片元素
    let img = document.getElementById('circulation-image');
    if (!img) {
        img = document.createElement('img');
        img.id = 'circulation-image';
        img.className = 'module-image';
        img.alt = '环流形势图';
        placeholder.parentNode.insertBefore(img, placeholder);
    }
    
    // 加载图片
    img.onload = function() {
        placeholder.style.display = 'none';
        img.style.display = 'block';
    };
    
    img.onerror = function() {
        placeholder.textContent = '图片加载失败: ' + filename;
        placeholder.style.display = 'block';
        img.style.display = 'none';
    };
    
    img.src = imagePath;
}

// 更新降水量预报图片
function updatePrecipitationImage() {
    const date = document.getElementById('precipitation-date').value;
    const time = document.getElementById('precipitation-time').value;
    const productBtn = document.querySelector('#precipitation-product-options .option-btn.active');
    const regionBtn = document.querySelector('#precipitation-region-options .option-btn.active');
    const periodBtn = document.querySelector('#precipitation-period-list .option-btn.active');
    
    const placeholder = document.getElementById('precipitation-placeholder');
    
    // 检查所有必要参数是否已选择
    if (!date || !time || !productBtn || !regionBtn || !periodBtn) {
        placeholder.textContent = '请选择所有必要参数';
        placeholder.style.display = 'block';
        return;
    }
    
    const product = productBtn.dataset.value;
    const region = regionBtn.dataset.value;
    const period = periodBtn.dataset.value.padStart(3, '0');
    
    // 构造图片文件名和路径（根据新目录结构调整）
    const dateTime = date.replace(/-/g, '') + time;
    const filename = `${product}_${region}_999hPa_${dateTime}_${period}.png`;
    const imagePath = `${IMAGE_BASE_PATH}${dateTime}/rain/${region}/${filename}`;
    
    // 创建或获取图片元素
    let img = document.getElementById('precipitation-image');
    if (!img) {
        img = document.createElement('img');
        img.id = 'precipitation-image';
        img.className = 'module-image';
        img.alt = '降水量预报图';
        placeholder.parentNode.insertBefore(img, placeholder);
    }
    
    // 加载图片
    img.onload = function() {
        placeholder.style.display = 'none';
        img.style.display = 'block';
    };
    
    img.onerror = function() {
        placeholder.textContent = '图片加载失败: ' + filename;
        placeholder.style.display = 'block';
        img.style.display = 'none';
    };
    
    img.src = imagePath;
}

// 更新要素预报图片
function updateElementImage() {
    const date = document.getElementById('element-date').value;
    const time = document.getElementById('element-time').value;
    const productBtn = document.querySelector('#element-product-options .option-btn.active');
    const regionBtn = document.querySelector('#element-region-options .option-btn.active');
    const periodBtn = document.querySelector('#element-period-list .option-btn.active');
    
    const placeholder = document.getElementById('element-placeholder');
    
    // 检查所有必要参数是否已选择
    if (!date || !time || !productBtn || !regionBtn || !periodBtn) {
        placeholder.textContent = '请选择所有必要参数';
        placeholder.style.display = 'block';
        return;
    }
    
    const product = productBtn.dataset.value;
    const region = regionBtn.dataset.value;
    const period = periodBtn.dataset.value.padStart(3, '0');
    
    // 构造图片文件名和路径
    const dateTime = date.replace(/-/g, '') + time;
    const filename = `${product}_${region}_${dateTime}_${period}.png`;
    const imagePath = `${IMAGE_BASE_PATH}${dateTime}/element/${region}/${filename}`;
    
    // 创建或获取图片元素
    let img = document.getElementById('element-image');
    if (!img) {
        img = document.createElement('img');
        img.id = 'element-image';
        img.className = 'module-image';
        img.alt = '要素预报图';
        placeholder.parentNode.insertBefore(img, placeholder);
    }
    
    // 加载图片
    img.onload = function() {
        placeholder.style.display = 'none';
        img.style.display = 'block';
    };
    
    img.onerror = function() {
        placeholder.textContent = '图片加载失败: ' + filename;
        placeholder.style.display = 'block';
        img.style.display = 'none';
    };
    
    img.src = imagePath;
}

// 获取默认起报时间
function getDefaultDateTime() {
    const now = new Date();
    const hours = now.getHours();
    let date = new Date();
    let time = '08';
    
    // 如果当前时间在16时之前，使用昨日20时
    if (hours < 16) {
        date.setDate(date.getDate() - 1);
        time = '20';
    }
    
    return {
        date: date.toISOString().split('T')[0],
        time: time
    };
}
// 站点数据（按省份组织）
const PROFILE_STATIONS = {
  "新疆": ["乌鲁木齐", "和田", "喀什", "哈密", "伊宁", "塔城", "阿勒泰", "博乐", "昌吉", "阿图什", "库尔勒", "阿克苏", "吐鲁番", "石河子"],
  "西藏": ["拉萨", "日喀则", "山南", "林芝", "昌都", "那曲", "噶尔"],
  "甘肃": ["兰州", "嘉峪关", "金昌", "白银", "天水", "武威", "张掖", "平凉", "酒泉", "庆阳", "定西", "陇南", "合作", "临夏"],
  "青海": ["西宁", "海东", "共和", "德令哈", "同仁", "玛沁", "玉树"],
  "宁夏": ["银川", "石嘴山", "吴忠", "固原", "中卫"],
  "四川": ["成都", "自贡", "攀枝花", "泸州", "德阳", "绵阳", "广元", "遂宁", "内江", "乐山", "南充", "眉山", "宜宾", "广安", "达州", "雅安", "巴中", "资阳", "马尔康", "康定", "西昌"],
  "重庆": ["沙坪坝", "酉阳", "万州"]
};

let currentProfileProvince = null;

// 初始化站点剖面模块
function initProfileModule() {
  const provinceContainer = document.getElementById('profile-province-options');
  const stationContainer = document.getElementById('profile-station-options');

  // 清空站点区
  function clearStations() {
    stationContainer.innerHTML = '';
    currentProfileProvince = null;
    document.getElementById('profile-placeholder').textContent = '请选择省份和站点';
    document.getElementById('profile-placeholder').style.display = 'block';
    const img = document.getElementById('profile-image');
    if (img) img.remove();
  }

  // 渲染省份按钮
  Object.keys(PROFILE_STATIONS).forEach(province => {
    const btn = document.createElement('button');
    btn.className = 'option-btn';
    btn.textContent = province;
    btn.dataset.value = province;
    btn.addEventListener('click', function () {
      // 激活省份
      document.querySelectorAll('#profile-province-options .option-btn').forEach(b => b.classList.remove('active'));
      this.classList.add('active');
      
      // 渲染对应站点
      stationContainer.innerHTML = '';
      currentProfileProvince = province;
      const stations = PROFILE_STATIONS[province];
      stations.forEach(station => {
        const sBtn = document.createElement('button');
        sBtn.className = 'option-btn';
        sBtn.textContent = station;
        sBtn.dataset.value = station;
        sBtn.addEventListener('click', function () {
          document.querySelectorAll('#profile-station-options .option-btn').forEach(b => b.classList.remove('active'));
          this.classList.add('active');
          updateProfileImage();
        });
        stationContainer.appendChild(sBtn);
      });
    });
    provinceContainer.appendChild(btn);
  });

  // 时间变化时清空站点
  document.getElementById('profile-date').addEventListener('change', clearStations);
  document.getElementById('profile-time').addEventListener('change', clearStations);
}

// 更新站点剖面图片
function updateProfileImage() {
  const date = document.getElementById('profile-date').value;
  const time = document.getElementById('profile-time').value;
  const provinceBtn = document.querySelector('#profile-province-options .option-btn.active');
  const stationBtn = document.querySelector('#profile-station-options .option-btn.active');
  const placeholder = document.getElementById('profile-placeholder');

  if (!date || !time || !provinceBtn || !stationBtn) {
    placeholder.textContent = '请选择所有必要参数';
    placeholder.style.display = 'block';
    return;
  }

  const province = provinceBtn.dataset.value;
  const station = stationBtn.dataset.value;
  const dateTime = date.replace(/-/g, '') + time;
  const filename = `profile_${province}_${station}_${dateTime}.png`;
  const imagePath = `./images/${dateTime}/profile/${province}/${filename}`;

  let img = document.getElementById('profile-image');
  if (!img) {
    img = document.createElement('img');
    img.id = 'profile-image';
    img.className = 'module-image';
    img.alt = '站点剖面图';
    placeholder.parentNode.insertBefore(img, placeholder);
  }

  img.onload = function () {
    placeholder.style.display = 'none';
    img.style.display = 'block';
  };
  img.onerror = function () {
    placeholder.textContent = '图片加载失败: ' + filename;
    placeholder.style.display = 'block';
    img.style.display = 'none';
  };
  img.src = imagePath;
}
// 页面加载完成后初始化
window.onload = function() {
    // 设置默认起报时间（已有）
  const { date, time } = getDefaultDateTime();
  document.getElementById('circulation-date').value = date;
  document.getElementById('precipitation-date').value = date;
  document.getElementById('element-date').value = date;
  document.getElementById('circulation-time').value = time;
  document.getElementById('precipitation-time').value = time;
  document.getElementById('element-time').value = time;

  // 新增：设置站点剖面默认时间
  document.getElementById('profile-date').value = date;
  document.getElementById('profile-time').value = time;

  // 初始化选项按钮（已有）
  initOptionButtons('circulation');
  initOptionButtons('precipitation');
  initOptionButtons('element');

  // 初始化预报时效（已有）
  initCirculationPeriods();
  initPrecipitationPeriods();
  initElementPeriods();

  // 新增：初始化站点剖面模块
  initProfileModule();

  // 初始加载图片（已有）
  setTimeout(() => {
    updateCirculationImage();
    updatePrecipitationImage();
    updateElementImage();
    // 注意：站点剖面需用户选择站点后才加载，无需初始加载
  }, 200);
};