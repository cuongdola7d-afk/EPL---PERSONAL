import { useId } from 'react'

export default function Pitch() {
  const gradient = useId()
  return <svg className="fantasy-field" viewBox="0 0 400 500" preserveAspectRatio="none" aria-hidden="true">
    <defs><linearGradient id={gradient} x1="0" y1="0" x2="0" y2="1"><stop stopColor="#4aae5e" /><stop offset="1" stopColor="#2f8c47" /></linearGradient></defs>
    <polygon points="36,0 364,0 400,500 0,500" fill={`url(#${gradient})`} />
    {[0, 2, 4].map((stripe) => <path key={stripe} d={`M${36 - stripe * 6} ${stripe * 83} L${364 + stripe * 6} ${stripe * 83} L${376 + stripe * 6} ${(stripe + 1) * 83} L${24 - stripe * 6} ${(stripe + 1) * 83} Z`} fill="#ffffff" opacity=".055" />)}
    <g fill="none" stroke="rgba(255,255,255,.64)" strokeWidth="2" strokeLinejoin="round">
      <polygon points="36,0 364,0 400,500 0,500" /><path d="M0 250 H400 M155 0 A45 32 0 0 0 245 0 M155 500 A45 32 0 0 1 245 500" />
      <ellipse cx="200" cy="250" rx="55" ry="55" />
      <path d="M96 0 V92 H304 V0 M139 0 V38 H261 V0 M75 500 V398 H325 V500 M139 500 V455 H261 V500" />
    </g><circle cx="200" cy="250" r="2.5" fill="white" opacity=".75" />
  </svg>
}
