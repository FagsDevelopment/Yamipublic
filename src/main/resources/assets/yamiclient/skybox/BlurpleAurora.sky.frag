// BlurpleAurora Made By : Dxchx
#version 150

uniform vec2 u_Resolution;
uniform float u_Time;
uniform mat4 u_InvVP;
uniform int u_WorldSpace;

out vec4 fragColor;

mat2 mm2(float a) {
    return mat2(cos(a), sin(a), -sin(a), cos(a));
}

float tri(float x) {
    return clamp(abs(fract(x) - 0.5), 0.01, 0.49);
}

float triNoise2d(vec2 p, float spd) {
    mat2 m2 = mat2(0.95534, 0.29552, -0.29552, 0.95534);
    float z = 1.8, z2 = 1.1, rz = 0.0;
    p *= mm2(p.x * 0.06);
    vec2 bp = p;
    for (int k = 0; k < 5; k++) {
        vec2 b = bp * 1.85;
        p -= vec2(tri(b.x) + tri(b.y), tri(b.y + tri(b.x))) * 0.75 * mm2(u_Time * spd) / z2;
        bp *= 1.3;
        z2 *= 0.45;
        z *= 0.42;
        p *= 1.21 + (rz - 1.0) * 0.02;
        rz += tri(p.x + tri(p.y)) * z;
        p *= -m2;
    }
    return clamp(1.0 / pow(rz * 29.0, 1.3), 0.0, 0.55);
}

float hash21(vec2 n) {
    return fract(sin(dot(n, vec2(12.9898, 4.1414))) * 43758.5453);
}

vec3 hash33(vec3 q) {
    q = fract(q * vec3(0.1031, 0.1030, 0.0973));
    q += dot(q, q.yxz + 33.33);
    return fract((q.xxy + q.yxx) * q.zyx);
}

vec4 aurora(vec3 ro, vec3 rd) {
    vec4 col = vec4(0.0), avg = vec4(0.0);
    float jit = hash21(gl_FragCoord.xy);
    float rdy = max(abs(rd.y), 0.02);
    for (int k = 0; k < 50; k++) {
        float i = float(k);
        float pt = (1.2 + pow(i, 1.4) * 0.003 - ro.y) / (rdy * 1.8 + 0.3) - 0.006 * jit * smoothstep(0.0, 15.0, i);
        float rzt = triNoise2d((ro + pt * rd).xz, 0.06);
        vec4 c = vec4((sin(2.0 - vec3(-10.0, -0.7, 1.3) + i * 0.03) * 0.5 + 0.5) * rzt, rzt);
        avg = mix(avg, c, 0.5);
        col += avg * exp2(-i * 0.065 - 2.5) * smoothstep(0.7, 5.0, i);
    }
    return col * clamp(rdy * 17.0 + 0.4, 0.0, 1.0) * 1.8;
}

vec3 stars(vec3 p) {
    vec3 c = vec3(0.0);
    float res = sqrt(u_Resolution.x / 400.0) * 400.0 * 0.15;
    for (int k = 0; k < 4; k++) {
        float i = float(k);
        vec2 rn = hash33(floor(p * res)).xy;
        float c2 = (1.0 - smoothstep(0.0, 0.7, length(fract(p * res) - 0.5))) * step(rn.x, 0.0005 + i * i * 0.001);
        c += c2 * (mix(vec3(1.0, 0.49, 0.1), vec3(0.75, 0.9, 1.0), rn.y) * 0.1 + 0.9);
        p *= 1.3;
    }
    return c * c * 0.8;
}

vec3 bg(vec3 rd) {
    float sd = pow(clamp(dot(normalize(vec3(-0.3, -0.6, 0.9)), rd) * 0.5 + 0.5, 0.0, 1.0), 5.0);
    return mix(vec3(0.1, 0.1, 0.2), vec3(0.8, 0.035, 0.15), sd) * 0.89;
}

void main() {
    vec2 ndc = (gl_FragCoord.xy / u_Resolution.xy) * 2.0 - 1.0;
    vec4 pNear = u_InvVP * vec4(ndc, -1.0, 1.0);
    vec4 pFar  = u_InvVP * vec4(ndc,  1.0, 1.0);
    vec3 rd = normalize((pFar.xyz / pFar.w) - (pNear.xyz / pNear.w));
    vec3 ro = vec3(0.0, 0.0, 0.0);

    float fade = smoothstep(0.0, 0.09, abs(rd.y)) * 0.1 + 0.9;
    vec3 col;

    if (rd.y > 0.0) {
        vec4 aur = smoothstep(0.0, 1.5, aurora(ro, rd)) * fade;
        col = bg(rd) * fade + (1.0 - fade) * 0.4;
        col += stars(normalize(rd));
        col = col * (1.0 - aur.a) + aur.rgb;
    } else {
        rd.y = abs(rd.y);
        col = bg(rd) * fade * 0.6;
        rd = normalize(rd);
        vec4 aur = smoothstep(0.0, 2.5, aurora(ro, rd));
        col += stars(rd) * 0.1;
        col = col * (1.0 - aur.a) + aur.rgb;
        float nz = triNoise2d((ro + (0.5 - ro.y) / rd.y * rd).xz * vec2(0.5, 0.7), 0.0);
        col += mix(vec3(0.016, 0.02, 0.04), vec3(0.21, 0.21, 0.35), nz * 0.4);
    }

    fragColor = vec4(col, 1.0);
}
