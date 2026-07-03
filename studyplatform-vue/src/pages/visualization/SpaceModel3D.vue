<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'
import { calculusModelOptions, physicsModelOptions, probabilityModelOptions, subjectOptions } from './spaceModelCatalog'

const canvasHost = ref(null)
const route = useRoute()
const router = useRouter()

const getModelsBySubject = (subjectId) => {
  if (subjectId === 'physics') return physicsModelOptions
  if (subjectId === 'probability') return probabilityModelOptions
  return calculusModelOptions
}

const resolveRouteSelection = () => {
  const requestedSubject = typeof route.query.subject === 'string' ? route.query.subject : ''
  const subjectId = subjectOptions.some((subject) => subject.id === requestedSubject)
    ? requestedSubject
    : 'probability'
  const models = getModelsBySubject(subjectId)
  const requestedModel = typeof route.query.model === 'string' ? route.query.model : ''
  const modelId = models.some((model) => model.id === requestedModel) ? requestedModel : models[0].id
  return { subjectId, modelId }
}

const initialSelection = resolveRouteSelection()

const state = reactive({
  subject: initialSelection.subjectId,
  modelId: initialSelection.modelId,
  amplitude: 1.2,
  domain: 5,
  resolution: 72,
  sliceLevel: 2,
  rho: 0.4,
  variance: 1.2,
  meanX: 0,
  meanY: 0,
  sampleSize: 40,
  wireframe: true,
  axes: true,
  autoRotate: true,
  question: '',
  lastQuestion: '',
})

const activeSubject = computed(() => subjectOptions.find((item) => item.id === state.subject) ?? subjectOptions[0])
const modelOptions = computed(() => getModelsBySubject(state.subject))
const activeModel = computed(() => modelOptions.value.find((item) => item.id === state.modelId) ?? modelOptions.value[0])
const activeExplanation = computed(() => {
  const shared = state.subject === 'physics'
    ? '你可以拖拽左侧三维视图改变观察角度，用滚轮缩放，并通过右侧滑块调节尺度、范围和模型精度。'
    : state.subject === 'probability'
      ? '你可以拖拽左侧三维视图改变观察角度，用滚轮缩放，并通过右侧参数调节相关系数、方差、均值和样本量。'
      : '你可以拖拽左侧三维视图改变观察角度，用滚轮缩放，并通过右侧滑块调节高度系数、定义域范围和曲面精度。'
  return `${activeModel.value.detail}${shared}`
})

const hyperbolaPreview = computed(() => {
  const width = 260
  const height = 220
  const centerX = width / 2
  const centerY = height / 2
  const domain = Number(state.domain)
  const level = Number(state.sliceLevel)
  const scale = 92 / domain
  const toSvg = (x, y) => `${(centerX + x * scale).toFixed(1)},${(centerY - y * scale).toFixed(1)}`
  const makePath = (points) => points.map((point, index) => `${index === 0 ? 'M' : 'L'} ${toSvg(point.x, point.y)}`).join(' ')
  const paths = []

  if (Math.abs(level) < 0.05) {
    paths.push(makePath([{ x: -domain, y: -domain }, { x: domain, y: domain }]))
    paths.push(makePath([{ x: -domain, y: domain }, { x: domain, y: -domain }]))
  } else if (level > 0) {
    ;[-1, 1].forEach((sign) => {
      const points = []
      for (let i = 0; i <= 140; i += 1) {
        const y = -domain + (i / 140) * domain * 2
        const x = sign * Math.sqrt(y * y + level)
        if (Math.abs(x) <= domain) points.push({ x, y })
      }
      if (points.length > 1) paths.push(makePath(points))
    })
  } else {
    ;[-1, 1].forEach((sign) => {
      const points = []
      for (let i = 0; i <= 140; i += 1) {
        const x = -domain + (i / 140) * domain * 2
        const y = sign * Math.sqrt(x * x - level)
        if (Math.abs(y) <= domain) points.push({ x, y })
      }
      if (points.length > 1) paths.push(makePath(points))
    })
  }

  return {
    width,
    height,
    axisX: `M 18 ${centerY} L ${width - 18} ${centerY}`,
    axisY: `M ${centerX} 18 L ${centerX} ${height - 18}`,
    paths,
    equation: `x² - y² = ${level.toFixed(1)}`,
  }
})

let renderer
let scene
let camera
let controls
let modelGroup
let axesHelper
let gridHelper
let frameId = 0
let resizeObserver
let clock

const disposeObject = (object) => {
  if (!object) return
  object.traverse?.((child) => {
    if (child.geometry) child.geometry.dispose()
    if (Array.isArray(child.material)) {
      child.material.forEach((material) => material.dispose())
    } else if (child.material) {
      child.material.dispose()
    }
  })
}

const toScenePoint = (x, z, y) => new THREE.Vector3(x, z, y)

const valueForFunctionSurface = (modelId, x, y) => {
  const a = Number(state.amplitude)
  if (modelId === 'paraboloid') return (a * (x * x + y * y)) / 10
  return (a * (x * x - y * y)) / 10
}

const buildIndexedSurface = (pointAt, rows, cols) => {
  const vertices = []
  const colors = []
  const indices = []
  const color = new THREE.Color()
  let minHeight = Infinity
  let maxHeight = -Infinity

  for (let row = 0; row <= rows; row += 1) {
    for (let col = 0; col <= cols; col += 1) {
      const point = pointAt(row / rows, col / cols)
      vertices.push(point.x, point.y, point.z)
      minHeight = Math.min(minHeight, point.y)
      maxHeight = Math.max(maxHeight, point.y)
    }
  }

  for (let row = 0; row <= rows; row += 1) {
    for (let col = 0; col <= cols; col += 1) {
      const height = vertices[(row * (cols + 1) + col) * 3 + 1]
      const t = maxHeight === minHeight ? 0.5 : (height - minHeight) / (maxHeight - minHeight)
      color.setHSL(0.62 - t * 0.5, 0.74, 0.54 + t * 0.08)
      colors.push(color.r, color.g, color.b)
    }
  }

  for (let row = 0; row < rows; row += 1) {
    for (let col = 0; col < cols; col += 1) {
      const a = row * (cols + 1) + col
      const b = a + 1
      const c = a + cols + 1
      const d = c + 1
      indices.push(a, c, b, b, c, d)
    }
  }

  const geometry = new THREE.BufferGeometry()
  geometry.setAttribute('position', new THREE.Float32BufferAttribute(vertices, 3))
  geometry.setAttribute('color', new THREE.Float32BufferAttribute(colors, 3))
  geometry.setIndex(indices)
  geometry.computeVertexNormals()
  return geometry
}

const createSurfaceMesh = (geometry, opacity = 1) => {
  const surface = new THREE.Mesh(
    geometry,
    new THREE.MeshStandardMaterial({
      vertexColors: true,
      metalness: 0.08,
      roughness: 0.34,
      side: THREE.DoubleSide,
      transparent: opacity < 1,
      opacity,
    }),
  )
  const wire = new THREE.Mesh(
    geometry.clone(),
    new THREE.MeshBasicMaterial({
      color: 0xffffff,
      transparent: true,
      opacity: 0.22,
      wireframe: true,
    }),
  )
  wire.userData.isWire = true
  return [surface, wire]
}

const createFunctionSurfaceGeometry = (modelId) => {
  const size = Number(state.domain)
  const segments = Number(state.resolution)
  return buildIndexedSurface((u, v) => {
    const x = -size + u * size * 2
    const y = -size + v * size * 2
    return toScenePoint(x, valueForFunctionSurface(modelId, x, y), y)
  }, segments, segments)
}

const createParametricSurfaceGeometry = (modelId) => {
  const segments = Number(state.resolution)
  if (modelId === 'sphere') {
    const r = 3.4
    return buildIndexedSurface((u, v) => {
      const theta = u * Math.PI * 2
      const phi = v * Math.PI
      return toScenePoint(r * Math.sin(phi) * Math.cos(theta), r * Math.cos(phi), r * Math.sin(phi) * Math.sin(theta))
    }, segments, segments)
  }

  if (modelId === 'cone') {
    const height = 4.8
    return buildIndexedSurface((u, v) => {
      const theta = u * Math.PI * 2
      const z = -height + v * height * 2
      const radius = Math.abs(z)
      return toScenePoint(radius * Math.cos(theta), z, radius * Math.sin(theta))
    }, segments, segments)
  }

  if (modelId === 'hyperboloid') {
    return buildIndexedSurface((u, v) => {
      const theta = u * Math.PI * 2
      const z = -3.6 + v * 7.2
      const radius = 1.5 * Math.sqrt(1 + (z * z) / 5.4)
      return toScenePoint(radius * Math.cos(theta), z, radius * Math.sin(theta))
    }, segments, segments)
  }

  return buildIndexedSurface((u, v) => {
    const theta = u * Math.PI * 2
    const phi = v * Math.PI
    return toScenePoint(4.2 * Math.sin(phi) * Math.cos(theta), 2.7 * Math.cos(phi), 3.2 * Math.sin(phi) * Math.sin(theta))
  }, segments, segments)
}

const createTubeFromPoints = (points, color = 0xec6ead, radius = 0.06) => {
  const curve = new THREE.CatmullRomCurve3(points)
  return new THREE.Mesh(
    new THREE.TubeGeometry(curve, Math.max(80, points.length * 2), radius, 12, false),
    new THREE.MeshStandardMaterial({ color, metalness: 0.1, roughness: 0.28 }),
  )
}

const createCylinderBetween = (start, end, radius, color, opacity = 1) => {
  const direction = end.clone().sub(start)
  const length = direction.length()
  const geometry = new THREE.CylinderGeometry(radius, radius, length, 18)
  const material = new THREE.MeshStandardMaterial({
    color,
    transparent: opacity < 1,
    opacity,
    roughness: 0.38,
    metalness: 0.08,
  })
  const mesh = new THREE.Mesh(geometry, material)
  mesh.position.copy(start).add(end).multiplyScalar(0.5)
  mesh.quaternion.setFromUnitVectors(new THREE.Vector3(0, 1, 0), direction.normalize())
  return mesh
}

const createPhysicsSphere = (radius, color, opacity = 1) => new THREE.Mesh(
  new THREE.SphereGeometry(radius, 32, 24),
  new THREE.MeshStandardMaterial({
    color,
    transparent: opacity < 1,
    opacity,
    roughness: 0.32,
    metalness: 0.06,
    side: THREE.DoubleSide,
  }),
)

const createTexturedPlane = (width, depth, color = 0x9fd6ff, opacity = 0.2) => {
  const mesh = new THREE.Mesh(
    new THREE.PlaneGeometry(width, depth, 1, 1),
    new THREE.MeshBasicMaterial({ color, transparent: true, opacity, side: THREE.DoubleSide }),
  )
  mesh.rotation.x = -Math.PI / 2
  return mesh
}

const addArrow = (group, origin, direction, length, color = 0xec6ead) => {
  const dir = direction.clone().normalize()
  const arrow = new THREE.ArrowHelper(dir, origin, length, color, length * 0.28, length * 0.16)
  group.add(arrow)
}

const normalDensity2D = (x, y) => {
  const sigma = Math.max(0.35, Number(state.variance))
  const rho = Math.max(-0.92, Math.min(0.92, Number(state.rho)))
  const dx = x - Number(state.meanX)
  const dy = y - Number(state.meanY)
  const denom = 2 * Math.PI * sigma * sigma * Math.sqrt(1 - rho * rho)
  const exponent = -((dx * dx - 2 * rho * dx * dy + dy * dy) / (2 * sigma * sigma * (1 - rho * rho)))
  return Math.exp(exponent) / denom
}

const createProbabilitySurfaceGeometry = (densityFn, heightScale = 18) => {
  const size = Number(state.domain)
  const segments = Number(state.resolution)
  return buildIndexedSurface((u, v) => {
    const x = -size + u * size * 2
    const y = -size + v * size * 2
    return toScenePoint(x, densityFn(x, y) * heightScale, y)
  }, segments, segments)
}

const buildProbabilityContourLines = (group, levels, densityFn, color = 0x1b1a55) => {
  const size = Number(state.domain)
  const samples = 240
  const material = new THREE.LineBasicMaterial({ color, transparent: true, opacity: 0.68 })
  levels.forEach((level) => {
    const points = []
    for (let i = 0; i <= samples; i += 1) {
      const angle = (i / samples) * Math.PI * 2
      const rho = Math.max(-0.92, Math.min(0.92, Number(state.rho)))
      const sigma = Math.max(0.35, Number(state.variance))
      const radius = Math.sqrt(Math.max(0.05, -2 * Math.log(level))) * sigma
      const major = radius * Math.sqrt(1 + rho)
      const minor = radius * Math.sqrt(1 - rho)
      const tilt = Math.PI / 4
      const px = major * Math.cos(angle)
      const py = minor * Math.sin(angle)
      const x = Number(state.meanX) + px * Math.cos(tilt) - py * Math.sin(tilt)
      const y = Number(state.meanY) + px * Math.sin(tilt) + py * Math.cos(tilt)
      if (Math.abs(x) <= size && Math.abs(y) <= size) points.push(toScenePoint(x, 0.045, y))
    }
    if (points.length > 3) group.add(new THREE.Line(new THREE.BufferGeometry().setFromPoints(points), material))
  })
}

const buildHydrogenOrbitals = (group) => {
  const nucleus = createPhysicsSphere(0.24, 0xffd166)
  group.add(nucleus)

  const sOrbital = createPhysicsSphere(1.25, 0x4cc9f0, 0.24)
  sOrbital.position.x = -4
  group.add(sOrbital)

  ;[-1, 1].forEach((sign) => {
    const lobe = createPhysicsSphere(0.86, sign > 0 ? 0xec6ead : 0x3494e6, 0.45)
    lobe.scale.set(0.72, 1.72, 0.72)
    lobe.position.set(0, sign * 1.15, 0)
    group.add(lobe)
  })

  const dCenters = [
    [-2.2, 0, 2.2],
    [2.2, 0, 2.2],
    [-2.2, 0, -2.2],
    [2.2, 0, -2.2],
  ]
  dCenters.forEach(([x, y, z], index) => {
    const lobe = createPhysicsSphere(0.72, index % 2 ? 0x4cc9f0 : 0xec6ead, 0.42)
    lobe.scale.set(1.45, 0.65, 0.82)
    lobe.position.set(x, y, z)
    lobe.lookAt(0, 0, 0)
    group.add(lobe)
  })
  const ring = new THREE.Mesh(
    new THREE.TorusGeometry(2.2, 0.035, 12, 96),
    new THREE.MeshStandardMaterial({ color: 0xffffff, transparent: true, opacity: 0.72 }),
  )
  ring.rotation.x = Math.PI / 2
  group.add(ring)
}

const buildElectricDipole = (group) => {
  const positive = createPhysicsSphere(0.36, 0xff4d6d)
  const negative = createPhysicsSphere(0.36, 0x3494e6)
  positive.position.x = -2.3
  negative.position.x = 2.3
  group.add(positive, negative)

  const surface = buildIndexedSurface((u, v) => {
    const x = -4.8 + u * 9.6
    const z = -4.8 + v * 9.6
    const y = 0.12 * (x * x - z * z)
    return new THREE.Vector3(x, y, z)
  }, 54, 54)
  createSurfaceMesh(surface, 0.34).forEach((mesh) => group.add(mesh))

  for (let i = 0; i < 14; i += 1) {
    const angle = (i / 14) * Math.PI * 2
    const side = new THREE.Vector3(0, Math.cos(angle), Math.sin(angle))
    const points = []
    for (let t = 0; t <= 1; t += 0.025) {
      const x = -2.3 + t * 4.6
      const bow = Math.sin(Math.PI * t) * 2.15
      points.push(new THREE.Vector3(x, side.y * bow, side.z * bow))
    }
    group.add(createTubeFromPoints(points, 0x1b1a55, 0.025))
  }
}

const buildSolenoidField = (group) => {
  const coilPoints = []
  const turns = 10
  for (let i = 0; i <= 640; i += 1) {
    const t = (i / 640) * Math.PI * 2 * turns
    const x = -4.8 + (i / 640) * 9.6
    coilPoints.push(new THREE.Vector3(x, Math.cos(t), Math.sin(t)))
  }
  group.add(createTubeFromPoints(coilPoints, 0xec6ead, 0.045))

  for (let z = -0.55; z <= 0.55; z += 0.55) {
    for (let y = -0.55; y <= 0.55; y += 0.55) {
      group.add(createTubeFromPoints([new THREE.Vector3(-4.5, y, z), new THREE.Vector3(4.5, y, z)], 0x3494e6, 0.028))
      addArrow(group, new THREE.Vector3(3.5, y, z), new THREE.Vector3(1, 0, 0), 0.8, 0x3494e6)
    }
  }

  ;[-1, 1].forEach((side) => {
    for (let i = 0; i < 5; i += 1) {
      const offset = -2 + i
      const points = []
      for (let t = 0; t <= 1; t += 0.02) {
        const theta = Math.PI * t
        points.push(new THREE.Vector3(4.5 - 9 * t, side * (2 + Math.sin(theta) * 0.6), offset * 0.45))
      }
      group.add(createTubeFromPoints(points, 0x8ecae6, 0.022))
    }
  })
}

const buildChargedParticleHelix = (group) => {
  const points = []
  const turns = 5.5
  for (let i = 0; i <= 420; i += 1) {
    const t = (i / 420) * Math.PI * 2 * turns
    points.push(new THREE.Vector3(0.16 * (t - Math.PI * turns), Math.cos(t) * 1.55, Math.sin(t) * 1.55))
  }
  group.add(createTubeFromPoints(points, 0xec6ead, 0.05))
  const particle = createPhysicsSphere(0.18, 0xffd166)
  group.add(particle)
  group.userData.update = (elapsed) => {
    const index = Math.floor((elapsed * 48) % points.length)
    particle.position.copy(points[index])
  }
  for (let x = -4; x <= 4; x += 2) addArrow(group, new THREE.Vector3(x, -2.6, -2.6), new THREE.Vector3(0, 1, 0), 1.35, 0x3494e6)
  addArrow(group, new THREE.Vector3(-4.8, 2.8, 2.6), new THREE.Vector3(1, 0, 0), 1.8, 0xec6ead)
}

const buildGyroscope = (group) => {
  const precession = new THREE.Group()
  group.add(precession)
  group.userData.update = (elapsed) => {
    precession.rotation.y = elapsed * 0.65
    rotor.rotation.y = elapsed * 8
    axle.rotation.z = 0.55 + Math.sin(elapsed * 2.2) * 0.08
  }

  const base = createPhysicsSphere(0.16, 0x1b1a55)
  group.add(base)
  const cone = new THREE.Line(
    new THREE.BufferGeometry().setFromPoints(Array.from({ length: 120 }, (_, i) => {
      const t = (i / 119) * Math.PI * 2
      return new THREE.Vector3(Math.cos(t) * 2.1, 2.8, Math.sin(t) * 2.1)
    })),
    new THREE.LineBasicMaterial({ color: 0x8ecae6 }),
  )
  group.add(cone)

  const axle = new THREE.Group()
  precession.add(axle)
  axle.rotation.z = 0.55
  axle.add(createCylinderBetween(new THREE.Vector3(0, 0, 0), new THREE.Vector3(0, 3.8, 0), 0.045, 0x1b1a55))
  const rotor = new THREE.Mesh(
    new THREE.CylinderGeometry(0.8, 0.8, 0.28, 48),
    new THREE.MeshStandardMaterial({ color: 0xec6ead, metalness: 0.18, roughness: 0.24 }),
  )
  rotor.position.y = 3.2
  axle.add(rotor)
  addArrow(axle, new THREE.Vector3(0, 3.6, 0), new THREE.Vector3(0, 1, 0), 1.2, 0x3494e6)
}

const buildElectromagneticWave = (group) => {
  const ePoints = []
  const bPoints = []
  const axisPoints = []
  for (let i = 0; i <= 320; i += 1) {
    const x = -5.5 + (i / 320) * 11
    const phase = x * 2.2
    ePoints.push(new THREE.Vector3(x, Math.sin(phase) * 1.4, 0))
    bPoints.push(new THREE.Vector3(x, 0, Math.sin(phase) * 1.4))
    axisPoints.push(new THREE.Vector3(x, 0, 0))
  }
  group.add(createTubeFromPoints(ePoints, 0xec6ead, 0.035))
  group.add(createTubeFromPoints(bPoints, 0x3494e6, 0.035))
  group.add(createTubeFromPoints(axisPoints, 0x1b1a55, 0.018))
  for (let x = -5; x <= 5; x += 2) {
    addArrow(group, new THREE.Vector3(x, 0, 0), new THREE.Vector3(0, Math.sin(x * 2.2), 0), 0.85, 0xec6ead)
    addArrow(group, new THREE.Vector3(x, 0, 0), new THREE.Vector3(0, 0, Math.sin(x * 2.2)), 0.85, 0x3494e6)
  }
}

const buildGasMotion = (group) => {
  const box = new THREE.LineSegments(
    new THREE.EdgesGeometry(new THREE.BoxGeometry(7.2, 4.6, 4.6)),
    new THREE.LineBasicMaterial({ color: 0x1b1a55, transparent: true, opacity: 0.55 }),
  )
  group.add(box)
  const molecules = []
  for (let i = 0; i < 46; i += 1) {
    const molecule = createPhysicsSphere(0.075, i % 2 ? 0xec6ead : 0x3494e6)
    molecule.position.set(
      ((i * 37) % 70) / 10 - 3.5,
      ((i * 23) % 42) / 10 - 2.1,
      ((i * 19) % 42) / 10 - 2.1,
    )
    molecule.userData.velocity = new THREE.Vector3(
      (((i * 13) % 20) - 10) / 42,
      (((i * 17) % 20) - 10) / 42,
      (((i * 29) % 20) - 10) / 42,
    )
    molecules.push(molecule)
    group.add(molecule)
  }
  group.userData.update = (_, delta) => {
    molecules.forEach((molecule) => {
      molecule.position.addScaledVector(molecule.userData.velocity, delta * 6)
      ;[['x', 3.5], ['y', 2.1], ['z', 2.1]].forEach(([axis, limit]) => {
        if (Math.abs(molecule.position[axis]) > limit) {
          molecule.position[axis] = Math.sign(molecule.position[axis]) * limit
          molecule.userData.velocity[axis] *= -1
        }
      })
    })
  }
}

const buildRigidBodyRotation = (group) => {
  const body = new THREE.Mesh(
    new THREE.BoxGeometry(2.5, 1.2, 1.7),
    new THREE.MeshStandardMaterial({ color: 0x8ecae6, metalness: 0.12, roughness: 0.3 }),
  )
  body.position.y = 1.6
  body.rotation.set(0.4, 0.2, 0.7)
  group.add(body)
  group.add(createCylinderBetween(new THREE.Vector3(0, 0, 0), new THREE.Vector3(0, 2.8, 0), 0.04, 0x1b1a55))
  addArrow(group, new THREE.Vector3(0, 2.8, 0), new THREE.Vector3(0.65, 1, 0.32), 1.6, 0xec6ead)
  group.userData.update = (elapsed) => {
    body.rotation.x = 0.4 + elapsed * 0.6
    body.rotation.y = 0.2 + elapsed * 0.9
    body.rotation.z = 0.7 + elapsed * 0.42
  }
}

const buildLissajous3D = (group) => {
  const points = []
  for (let i = 0; i <= 900; i += 1) {
    const t = (i / 900) * Math.PI * 2
    points.push(new THREE.Vector3(3.5 * Math.sin(3 * t), 2.6 * Math.sin(4 * t + Math.PI / 3), 3.5 * Math.sin(5 * t + Math.PI / 5)))
  }
  group.add(createTubeFromPoints(points, 0xec6ead, 0.045))
  group.add(createPhysicsSphere(0.12, 0xffd166))
  group.userData.update = (elapsed) => {
    const t = (elapsed * 0.18) % 1
    group.children[group.children.length - 1].position.copy(points[Math.floor(t * (points.length - 1))])
  }
}

const buildPointChargeShells = (group) => {
  group.add(createPhysicsSphere(0.28, 0xff4d6d))
  ;[1.2, 2.1, 3.1, 4.2].forEach((radius, index) => {
    group.add(createPhysicsSphere(radius, index % 2 ? 0x3494e6 : 0xec6ead, 0.12))
  })
  for (let i = 0; i < 14; i += 1) {
    const theta = (i / 14) * Math.PI * 2
    const direction = new THREE.Vector3(Math.cos(theta), 0.35 * Math.sin(theta * 2), Math.sin(theta)).normalize()
    addArrow(group, direction.clone().multiplyScalar(0.6), direction, 2.4, 0x1b1a55)
  }
}

const buildBivariateNormal = (group) => {
  const geometry = createProbabilitySurfaceGeometry(normalDensity2D, 18 * Number(state.amplitude))
  createSurfaceMesh(geometry, 0.92).forEach((mesh) => group.add(mesh))
  buildProbabilityContourLines(group, [0.35, 0.55, 0.72, 0.86], normalDensity2D)
  const meanMarker = createPhysicsSphere(0.13, 0xffd166)
  meanMarker.position.set(Number(state.meanX), normalDensity2D(Number(state.meanX), Number(state.meanY)) * 18 * Number(state.amplitude) + 0.18, Number(state.meanY))
  group.add(meanMarker)
}

const buildUniformPlateau = (group) => {
  const width = Number(state.domain) * 1.25
  const depth = Number(state.domain)
  const height = 1.1 * Number(state.amplitude)
  const top = new THREE.Mesh(
    new THREE.BoxGeometry(width, height, depth),
    new THREE.MeshStandardMaterial({ color: 0x8ecae6, transparent: true, opacity: 0.62, roughness: 0.34 }),
  )
  top.position.y = height / 2
  group.add(top)
  group.add(new THREE.LineSegments(new THREE.EdgesGeometry(top.geometry), new THREE.LineBasicMaterial({ color: 0x1b1a55 })))
  group.children[group.children.length - 1].position.copy(top.position)
  group.add(createTexturedPlane(width, depth, 0xec6ead, 0.14))
}

const buildJointCdf = (group) => {
  const size = Number(state.domain)
  const geometry = buildIndexedSurface((u, v) => {
    const x = -size + u * size * 2
    const y = -size + v * size * 2
    const fx = 1 / (1 + Math.exp(-1.15 * (x - Number(state.meanX))))
    const fy = 1 / (1 + Math.exp(-1.15 * (y - Number(state.meanY))))
    return toScenePoint(x, fx * fy * 4.6 * Number(state.amplitude), y)
  }, Number(state.resolution), Number(state.resolution))
  createSurfaceMesh(geometry, 0.9).forEach((mesh) => group.add(mesh))
}

const buildProbabilitySolid = (group) => {
  buildBivariateNormal(group)
  const size = Number(state.domain)
  const rx = Math.min(2.4, size * 0.48)
  const rz = Math.min(1.8, size * 0.38)
  const material = new THREE.MeshStandardMaterial({ color: 0xec6ead, transparent: true, opacity: 0.28, roughness: 0.36 })
  for (let ix = -3; ix <= 3; ix += 1) {
    for (let iz = -2; iz <= 2; iz += 1) {
      const x = (ix / 3) * rx
      const z = (iz / 2) * rz
      const height = normalDensity2D(x, z) * 18 * Number(state.amplitude)
      const column = new THREE.Mesh(new THREE.BoxGeometry(rx / 3.4, Math.max(0.05, height), rz / 2.8), material.clone())
      column.position.set(x, height / 2, z)
      group.add(column)
    }
  }
  const region = createTexturedPlane(rx * 2.35, rz * 2.35, 0x1b1a55, 0.18)
  region.position.y = 0.03
  group.add(region)
}

const buildRhoContour = (group) => {
  const plane = createTexturedPlane(Number(state.domain) * 2, Number(state.domain) * 2, 0xeff6ff, 0.46)
  group.add(plane)
  buildProbabilityContourLines(group, [0.28, 0.45, 0.62, 0.78, 0.9], normalDensity2D, 0xec6ead)
  const ridge = createProbabilitySurfaceGeometry(normalDensity2D, 8 * Number(state.amplitude))
  createSurfaceMesh(ridge, 0.3).forEach((mesh) => group.add(mesh))
}

const buildNormalConvergence = (group) => {
  const n = Number(state.sampleSize)
  const bins = Math.min(15, Math.max(5, Math.round(n / 8)))
  const width = 7 / bins
  const material = new THREE.MeshStandardMaterial({ color: 0x3494e6, transparent: true, opacity: 0.62, roughness: 0.4 })
  for (let i = 0; i < bins; i += 1) {
    for (let j = 0; j < bins; j += 1) {
      const x = -3.5 + i * width + width / 2
      const z = -3.5 + j * width + width / 2
      const base = normalDensity2D(x, z) * 15
      const noise = (Math.sin(i * 12.989 + j * 78.23 + n * 0.21) * 0.5 + 0.5) * (18 / Math.max(n, 10))
      const height = Math.max(0.03, base + noise)
      const column = new THREE.Mesh(new THREE.BoxGeometry(width * 0.78, height, width * 0.78), material.clone())
      column.position.set(x, height / 2, z)
      group.add(column)
    }
  }
  const target = createProbabilitySurfaceGeometry(normalDensity2D, 15)
  createSurfaceMesh(target, 0.32).forEach((mesh) => group.add(mesh))
}

const buildNormalEllipsoids = (group) => {
  ;[
    { radius: 1.2, opacity: 0.34, color: 0xec6ead },
    { radius: 2.1, opacity: 0.2, color: 0x3494e6 },
    { radius: 3.1, opacity: 0.13, color: 0x8ecae6 },
  ].forEach((shell) => {
    const ellipsoid = createPhysicsSphere(shell.radius, shell.color, shell.opacity)
    ellipsoid.scale.set(1.55, 0.85 + Number(state.variance) * 0.18, 1.05)
    ellipsoid.rotation.y = Number(state.rho) * 0.9
    ellipsoid.rotation.z = -Number(state.rho) * 0.45
    group.add(ellipsoid)
  })
  addArrow(group, new THREE.Vector3(0, 0, 0), new THREE.Vector3(1.4, 0.25, 0.3), 2.1, 0xec6ead)
  addArrow(group, new THREE.Vector3(0, 0, 0), new THREE.Vector3(-0.25, 1.1, 0.5), 1.7, 0x3494e6)
}

const buildCurveModel = (group, spring = false) => {
  const turns = spring ? 7.5 : 3.8
  const radius = spring ? 2.1 : 2.6
  const pitch = spring ? 0.18 : 0.42
  const points = []
  for (let i = 0; i <= 420; i += 1) {
    const t = (i / 420) * Math.PI * 2 * turns
    points.push(toScenePoint(radius * Math.cos(t), pitch * (t - Math.PI * turns), radius * Math.sin(t)))
  }
  group.add(createTubeFromPoints(points, spring ? 0x3494e6 : 0xec6ead, spring ? 0.045 : 0.07))

  if (spring) {
    const t = Math.PI * 2 * turns * 0.58
    const point = toScenePoint(radius * Math.cos(t), pitch * (t - Math.PI * turns), radius * Math.sin(t))
    const tangent = new THREE.Vector3(-radius * Math.sin(t), pitch, radius * Math.cos(t)).normalize()
    addArrow(group, point.clone().add(tangent.clone().multiplyScalar(-1.4)), tangent, 2.8, 0x1b1a55)
    group.add(new THREE.Mesh(new THREE.SphereGeometry(0.16, 20, 20), new THREE.MeshStandardMaterial({ color: 0x1b1a55 })))
    group.children[group.children.length - 1].position.copy(point)
  }
}

const buildIntegralSolid = (group) => {
  const size = 4.8
  const topGeometry = buildIndexedSurface((u, v) => {
    const x = -size + u * size * 2
    const y = -size + v * size * 2
    const height = 1.2 + Number(state.amplitude) * (1.2 + 0.45 * Math.sin(x * 0.85) * Math.cos(y * 0.85))
    return toScenePoint(x, height, y)
  }, Number(state.resolution), Number(state.resolution))
  createSurfaceMesh(topGeometry, 0.86).forEach((mesh) => group.add(mesh))
  group.add(createTexturedPlane(size * 2, size * 2, 0x3494e6, 0.13))

  const columnMaterial = new THREE.MeshBasicMaterial({ color: 0xec6ead, transparent: true, opacity: 0.14, side: THREE.DoubleSide })
  const step = size / 2
  for (let x = -size; x <= size; x += step) {
    const h1 = 1.2 + Number(state.amplitude) * (1.2 + 0.45 * Math.sin(x * 0.85) * Math.cos(-size * 0.85))
    const h2 = 1.2 + Number(state.amplitude) * (1.2 + 0.45 * Math.sin(x * 0.85) * Math.cos(size * 0.85))
    const wall = new THREE.Mesh(new THREE.PlaneGeometry(size * 2, Math.max(h1, h2)), columnMaterial.clone())
    wall.position.set(x, Math.max(h1, h2) / 2, 0)
    wall.rotation.y = Math.PI / 2
    group.add(wall)
  }
}

const buildGradientContour = (group) => {
  const size = Number(state.domain)
  const surfaceGeometry = createFunctionSurfaceGeometry('saddle')
  createSurfaceMesh(surfaceGeometry, 0.42).forEach((mesh) => group.add(mesh))
  group.add(createTexturedPlane(size * 2, size * 2, 0xeff6ff, 0.35))

  const contourMaterial = new THREE.LineBasicMaterial({ color: 0x1b1a55, transparent: true, opacity: 0.72 })
  ;[-8, -4, 0, 4, 8].forEach((level) => {
    const points = []
    for (let i = 0; i <= 180; i += 1) {
      const x = -size + (i / 180) * size * 2
      const y2 = x * x - level
      if (y2 >= 0 && y2 <= size * size) {
        const y = Math.sqrt(y2)
        points.push(toScenePoint(x, 0.035, y))
      }
    }
    if (points.length > 1) group.add(new THREE.Line(new THREE.BufferGeometry().setFromPoints(points), contourMaterial))
    const mirror = points.map((point) => toScenePoint(point.x, point.y, -point.z))
    if (mirror.length > 1) group.add(new THREE.Line(new THREE.BufferGeometry().setFromPoints(mirror), contourMaterial))
  })

  for (let x = -4; x <= 4; x += 2) {
    for (let y = -4; y <= 4; y += 2) {
      const gradient = new THREE.Vector3(2 * x, 0, -2 * y)
      if (gradient.length() > 0.01) addArrow(group, toScenePoint(x, 0.08, y), gradient, 0.75, 0xec6ead)
    }
  }
}

const buildSaddleTangent = (group) => {
  createSurfaceMesh(createFunctionSurfaceGeometry('saddle'), 0.86).forEach((mesh) => group.add(mesh))
  const domain = Number(state.domain)
  const level = Number(state.sliceLevel)
  const renderedLevel = (Number(state.amplitude) * level) / 10
  const plane = createTexturedPlane(domain * 2.1, domain * 2.1, 0x1b1a55, 0.2)
  plane.position.y = renderedLevel
  group.add(plane)

  const makeBranch = (points) => {
    if (points.length < 2) return
    group.add(createTubeFromPoints(points, 0xec6ead, 0.055))
  }

  if (Math.abs(level) < 0.05) {
    makeBranch([
      toScenePoint(-domain, renderedLevel + 0.035, -domain),
      toScenePoint(domain, renderedLevel + 0.035, domain),
    ])
    makeBranch([
      toScenePoint(-domain, renderedLevel + 0.035, domain),
      toScenePoint(domain, renderedLevel + 0.035, -domain),
    ])
  } else if (level > 0) {
    ;[-1, 1].forEach((sign) => {
      const points = []
      for (let i = 0; i <= 180; i += 1) {
        const y = -domain + (i / 180) * domain * 2
        const x = sign * Math.sqrt(y * y + level)
        if (Math.abs(x) <= domain) points.push(toScenePoint(x, renderedLevel + 0.035, y))
      }
      makeBranch(points)
    })
  } else {
    ;[-1, 1].forEach((sign) => {
      const points = []
      for (let i = 0; i <= 180; i += 1) {
        const x = -domain + (i / 180) * domain * 2
        const y = sign * Math.sqrt(x * x - level)
        if (Math.abs(y) <= domain) points.push(toScenePoint(x, renderedLevel + 0.035, y))
      }
      makeBranch(points)
    })
  }
}

const buildPhysicsModel = (group, kind) => {
  if (kind === 'hydrogenOrbitals') buildHydrogenOrbitals(group)
  else if (kind === 'electricDipole') buildElectricDipole(group)
  else if (kind === 'solenoidField') buildSolenoidField(group)
  else if (kind === 'chargedParticleHelix') buildChargedParticleHelix(group)
  else if (kind === 'gyroscope') buildGyroscope(group)
  else if (kind === 'emWave') buildElectromagneticWave(group)
  else if (kind === 'gasMotion') buildGasMotion(group)
  else if (kind === 'rigidBodyRotation') buildRigidBodyRotation(group)
  else if (kind === 'lissajous3d') buildLissajous3D(group)
  else if (kind === 'pointChargeShells') buildPointChargeShells(group)
}

const buildProbabilityModel = (group, kind) => {
  if (kind === 'bivariateNormal') buildBivariateNormal(group)
  else if (kind === 'uniformPlateau') buildUniformPlateau(group)
  else if (kind === 'jointCdf') buildJointCdf(group)
  else if (kind === 'probabilitySolid') buildProbabilitySolid(group)
  else if (kind === 'rhoContour') buildRhoContour(group)
  else if (kind === 'normalConvergence') buildNormalConvergence(group)
  else if (kind === 'normalEllipsoids') buildNormalEllipsoids(group)
}

const rebuildModel = () => {
  if (!scene) return
  if (modelGroup) {
    scene.remove(modelGroup)
    disposeObject(modelGroup)
  }

  modelGroup = new THREE.Group()
  const kind = activeModel.value.kind

  if (state.subject === 'physics') {
    buildPhysicsModel(modelGroup, kind)
  } else if (state.subject === 'probability') {
    buildProbabilityModel(modelGroup, kind)
  } else if (kind === 'functionSurface') {
    createSurfaceMesh(createFunctionSurfaceGeometry(activeModel.value.id)).forEach((mesh) => modelGroup.add(mesh))
  } else if (kind === 'parametricSurface') {
    createSurfaceMesh(createParametricSurfaceGeometry(activeModel.value.id)).forEach((mesh) => modelGroup.add(mesh))
  } else if (kind === 'curve') {
    buildCurveModel(modelGroup, false)
  } else if (kind === 'spring') {
    buildCurveModel(modelGroup, true)
  } else if (kind === 'solid') {
    buildIntegralSolid(modelGroup)
  } else if (kind === 'vectorField') {
    buildGradientContour(modelGroup)
  } else if (kind === 'tangentPlane') {
    buildSaddleTangent(modelGroup)
  }

  scene.add(modelGroup)
  updateHelpers()
}

const updateHelpers = () => {
  if (!scene) return
  if (axesHelper) axesHelper.visible = state.axes
  if (gridHelper) gridHelper.visible = state.axes
  modelGroup?.traverse((child) => {
    if (child.userData?.isWire) child.visible = state.wireframe
  })
}

const resizeRenderer = () => {
  if (!canvasHost.value || !renderer || !camera) return
  const { width, height } = canvasHost.value.getBoundingClientRect()
  if (!width || !height) return
  renderer.setSize(width, height, false)
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  camera.aspect = width / height
  camera.updateProjectionMatrix()
}

const animate = () => {
  frameId = requestAnimationFrame(animate)
  const delta = clock?.getDelta() ?? 0
  const elapsed = clock?.elapsedTime ?? 0
  if (controls) {
    controls.autoRotate = state.autoRotate
    controls.update()
  }
  modelGroup?.userData?.update?.(elapsed, delta)
  renderer?.render(scene, camera)
}

const initScene = async () => {
  await nextTick()
  if (!canvasHost.value) return

  scene = new THREE.Scene()
  scene.background = new THREE.Color(0xf9fbff)
  clock = new THREE.Clock()

  camera = new THREE.PerspectiveCamera(42, 1, 0.1, 100)
  camera.position.set(9, 7.5, 10)

  renderer = new THREE.WebGLRenderer({ antialias: true, preserveDrawingBuffer: true })
  renderer.outputColorSpace = THREE.SRGBColorSpace
  renderer.shadowMap.enabled = true
  canvasHost.value.appendChild(renderer.domElement)

  controls = new OrbitControls(camera, renderer.domElement)
  controls.enableDamping = true
  controls.dampingFactor = 0.08
  controls.autoRotateSpeed = 0.9
  controls.target.set(0, 0.4, 0)

  scene.add(new THREE.HemisphereLight(0xffffff, 0x405178, 1.6))
  const keyLight = new THREE.DirectionalLight(0xffffff, 1.35)
  keyLight.position.set(4, 9, 7)
  scene.add(keyLight)
  const fillLight = new THREE.PointLight(0xec6ead, 1.1, 32)
  fillLight.position.set(-6, 4, -5)
  scene.add(fillLight)

  axesHelper = new THREE.AxesHelper(7.4)
  scene.add(axesHelper)
  gridHelper = new THREE.GridHelper(14, 14, 0x9bb8d8, 0xd8e3f1)
  scene.add(gridHelper)

  rebuildModel()
  resizeRenderer()
  resizeObserver = new ResizeObserver(resizeRenderer)
  resizeObserver.observe(canvasHost.value)
  animate()
}

const setSubject = (subjectId) => {
  if (state.subject === subjectId) return
  const nextOptions = getModelsBySubject(subjectId)
  state.subject = subjectId
  state.modelId = nextOptions[0].id
  state.question = ''
  state.lastQuestion = ''
}

const submitQuestion = () => {
  const content = state.question.trim()
  if (!content) return
  state.lastQuestion = content
  state.question = ''
  // TODO: 接入后端问答接口，例如 POST /api/visualization/space-3d/questions
}

watch(
  () => [
    state.subject,
    state.modelId,
    state.amplitude,
    state.domain,
    state.resolution,
    state.sliceLevel,
    state.rho,
    state.variance,
    state.meanX,
    state.meanY,
    state.sampleSize,
  ],
  rebuildModel,
)

watch(
  () => [route.query.subject, route.query.model],
  () => {
    const next = resolveRouteSelection()
    if (state.subject !== next.subjectId) state.subject = next.subjectId
    if (state.modelId !== next.modelId) state.modelId = next.modelId
  },
)

watch(
  () => [state.subject, state.modelId],
  ([subject, model]) => {
    if (route.query.subject === subject && route.query.model === model) return
    router.replace({
      path: '/visualization/space-3d',
      query: { subject, model },
    })
  },
)

watch(
  () => [state.wireframe, state.axes],
  updateHelpers,
)

onMounted(initScene)

onBeforeUnmount(() => {
  cancelAnimationFrame(frameId)
  resizeObserver?.disconnect()
  controls?.dispose()
  if (modelGroup) disposeObject(modelGroup)
  renderer?.dispose()
  renderer?.domElement?.remove()
})
</script>

<template>
  <main class="visual-page space-model-page">
    <nav class="algorithm-viewer-breadcrumb visual-page-breadcrumb" aria-label="当前位置">
      <RouterLink to="/visualization">可视化</RouterLink>
      <span>&gt;</span>
      <RouterLink to="/visualization/space-models">空间模型实验室</RouterLink>
      <span>&gt;</span>
      <RouterLink :to="{ path: '/visualization/space-3d', query: { subject: state.subject, model: modelOptions[0].id } }">
        {{ activeSubject.label }}
      </RouterLink>
      <span>&gt;</span>
      <strong>{{ activeModel.name }}</strong>
    </nav>

    <section class="visual-hero compact space-hero">
      <p class="visual-kicker">{{ activeSubject.kicker }}</p>
      <h1>{{ activeSubject.title }}</h1>
      <p>{{ activeSubject.intro }}</p>
      <div class="space-subject-switch" aria-label="三维模型学科切换">
        <button
          v-for="subject in subjectOptions"
          :key="subject.id"
          type="button"
          :class="{ active: state.subject === subject.id }"
          @click="setSubject(subject.id)"
        >
          {{ subject.label }}
        </button>
      </div>
    </section>

    <section class="calculus-3d-workbench" :aria-label="`${activeSubject.title}实验台`">
      <div class="calculus-scene-stage">
        <div ref="canvasHost" class="calculus-canvas-host" aria-label="三维函数曲面"></div>
        <div class="calculus-scene-badge">
          <span>{{ activeModel.formula }}</span>
          <strong>{{ activeModel.name }}</strong>
        </div>
      </div>

      <aside class="calculus-control-panel" aria-label="三维曲面控制面板">
        <div class="calculus-panel-head">
          <p>{{ state.subject === 'physics' ? 'Physics Control' : 'Surface Control' }}</p>
          <h2>{{ activeModel.name }}</h2>
          <span>{{ activeModel.description }}</span>
        </div>

        <label>
          <span>模型分类</span>
          <select v-model="state.modelId">
            <option v-for="model in modelOptions" :key="model.id" :value="model.id">
              {{ model.name }}
            </option>
          </select>
        </label>

        <label>
          <span>振幅 / 高度系数：{{ state.amplitude.toFixed(1) }}</span>
          <input v-model.number="state.amplitude" type="range" min="0.4" max="2.8" step="0.1">
        </label>

        <label>
          <span>定义域范围：[-{{ state.domain }}, {{ state.domain }}]</span>
          <input v-model.number="state.domain" type="range" min="3" max="8" step="1">
        </label>

        <label>
          <span>曲面精度：{{ state.resolution }}</span>
          <input v-model.number="state.resolution" type="range" min="32" max="96" step="8">
        </label>

        <template v-if="state.subject === 'probability'">
          <label v-if="['bivariate-normal', 'rho-contour', 'normal-ellipsoids'].includes(activeModel.id)">
            <span>相关系数 ρ：{{ state.rho.toFixed(2) }}</span>
            <input v-model.number="state.rho" type="range" min="-0.9" max="0.9" step="0.05">
          </label>

          <label v-if="['bivariate-normal', 'normal-convergence', 'normal-ellipsoids'].includes(activeModel.id)">
            <span>方差 σ²：{{ state.variance.toFixed(1) }}</span>
            <input v-model.number="state.variance" type="range" min="0.4" max="3" step="0.1">
          </label>

          <div v-if="activeModel.id === 'bivariate-normal'" class="probability-mean-grid">
            <label>
              <span>均值 μx：{{ state.meanX.toFixed(1) }}</span>
              <input v-model.number="state.meanX" type="range" min="-2.5" max="2.5" step="0.1">
            </label>
            <label>
              <span>均值 μy：{{ state.meanY.toFixed(1) }}</span>
              <input v-model.number="state.meanY" type="range" min="-2.5" max="2.5" step="0.1">
            </label>
          </div>

          <label v-if="activeModel.id === 'normal-convergence'">
            <span>样本量 n：{{ state.sampleSize }}</span>
            <input v-model.number="state.sampleSize" type="range" min="10" max="160" step="10">
          </label>
        </template>

        <label v-if="activeModel.id === 'saddle-tangent'">
          <span>切面高度 c：{{ state.sliceLevel.toFixed(1) }}</span>
          <input v-model.number="state.sliceLevel" type="range" min="-8" max="8" step="0.2">
        </label>

        <div v-if="activeModel.id === 'saddle-tangent'" class="calculus-hyperbola-preview">
          <div>
            <strong>实时截线图</strong>
            <span>{{ hyperbolaPreview.equation }}</span>
          </div>
          <svg
            :viewBox="`0 0 ${hyperbolaPreview.width} ${hyperbolaPreview.height}`"
            role="img"
            aria-label="马鞍面水平切面截出的双曲线"
          >
            <path class="axis" :d="hyperbolaPreview.axisX" />
            <path class="axis" :d="hyperbolaPreview.axisY" />
            <path
              v-for="path in hyperbolaPreview.paths"
              :key="path"
              class="curve"
              :d="path"
            />
          </svg>
        </div>

        <div class="calculus-toggle-row">
          <label>
            <input v-model="state.wireframe" type="checkbox">
            <span>显示网格线</span>
          </label>
          <label>
            <input v-model="state.axes" type="checkbox">
            <span>显示坐标轴</span>
          </label>
          <label>
            <input v-model="state.autoRotate" type="checkbox">
            <span>自动旋转</span>
          </label>
        </div>

        <div class="calculus-note">
          <strong>教学提示</strong>
          <p>{{ activeModel.detail }}</p>
        </div>
      </aside>
    </section>

    <section class="calculus-learning-zone" aria-label="三维图像讲解和问答">
      <article class="calculus-explanation-card">
        <p>Detailed Explanation</p>
        <h2>{{ activeModel.name }}讲解</h2>
        <div class="calculus-explanation-content">
          <p><strong>公式：</strong>{{ activeModel.formula }}</p>
          <p>{{ activeExplanation }}</p>
          <p>
            学习时建议先打开“显示坐标轴”和“显示网格线”，观察模型与坐标平面的相对位置；
            再关闭自动旋转，手动拖拽到正视、侧视和俯视角度，对比不同视角下的截线形态。
          </p>
          <p>
            对于二次曲面，核心是看截线：固定一个变量后，空间曲面会退化为平面曲线。
            对于空间曲线，核心是看参数 t 增大时点的运动轨迹。对于梯度场，核心是比较箭头方向与等高线的垂直关系。
          </p>
        </div>
      </article>

      <article class="calculus-qa-card">
        <p>Q&A Interface</p>
        <h2>问答区</h2>
        <span>这里预留后端问答接口，后续可接入 AI 助教、课程题库或教师答疑 API。</span>
        <textarea
          v-model="state.question"
          rows="5"
          :placeholder="state.subject === 'physics' ? '输入你想问的问题，例如：为什么电磁波中 E、B、k 两两垂直？' : '输入你想问的问题，例如：为什么马鞍面的原点不是极值点？'"
        ></textarea>
        <button type="button" @click="submitQuestion">提交问题</button>
        <div v-if="state.lastQuestion" class="calculus-question-preview">
          <strong>已暂存的问题</strong>
          <p>{{ state.lastQuestion }}</p>
        </div>
      </article>
    </section>
  </main>
</template>
