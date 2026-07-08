import { FC, useEffect, useState } from 'react';
import Particles, { initParticlesEngine } from '@tsparticles/react';
import type { Container, Engine } from '@tsparticles/engine';
import { loadSlim } from '@tsparticles/slim';

import banner from 'public/sciserver_banner.jpg';

const particlesLoaded = async (container?: Container) => {
  console.log(container);
};

export const ParticlesComp: FC = () => {

  const [init, setInit] = useState(false);

  // ON MOUNT: UI config
  useEffect(() => {
    initParticlesEngine(async (engine: Engine) => {
      await loadSlim(engine);
      setInit(true);
    });
  }, []);



  return <div>
    {init && <Particles
      id="tsparticles"
      particlesLoaded={particlesLoaded}
      options={{
        background: { image: `linear-gradient(rgba(0, 0, 0, 0.3), rgba(0, 0, 0, 0.3)), url(${banner.src})` },
        fpsLimit: 120,
        interactivity: {
          events: {
            onClick: {
              enable: true,
              mode: 'push'
            },
            onHover: {
              enable: true,
              mode: 'repulse'
            },
            resize: { enable: true }
          },
          modes: {
            push: { quantity: 4 },
            repulse: {
              distance: 200,
              duration: 0.4
            }
          }
        },
        particles: {
          color: { value: '#ffffff' },
          links: {
            color: '#ffffff',
            distance: 150,
            enable: true,
            opacity: 0.5,
            width: 1
          },
          move: {
            direction: 'none',
            enable: true,
            outModes: { default: 'bounce' },
            random: false,
            speed: 6,
            straight: false
          },
          number: {
            density: {
              enable: true,
              height: 800,
              width: 800
            },
            value: 80
          },
          opacity: { value: 0.5 },
          shape: { type: 'circle' },
          size: { value: { min: 1, max: 5 } }
        },
        detectRetina: true
      }}
    />}
  </div>;
};
