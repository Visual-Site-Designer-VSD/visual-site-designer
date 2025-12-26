/**
 * Pre-built Full Page UI Templates
 * These templates are complete page layouts that can be customized after dropping
 * Each template has a SINGLE root container with all sections as children
 */

import { UITemplate, TemplateGroup } from '../types/templates';

/**
 * Homepage Templates
 */
const homepageTemplates: UITemplate[] = [
  {
    id: 'homepage-business',
    name: 'Business Homepage',
    category: 'hero',
    icon: '🏢',
    description: 'Professional business homepage with hero, features, stats, testimonial and footer',
    tags: ['homepage', 'business', 'corporate', 'landing'],
    suggestedSize: { columnSpan: 12, rowSpan: 1 },
    components: [
      // Single root container wrapping all page sections
      {
        pluginId: 'core-ui',
        componentId: 'Container',
        props: { layoutMode: 'flex-column', gap: '0px' },
        styles: { width: '100%', minHeight: '100vh' },
        children: [
          // Navbar
          {
            pluginId: 'core-navbar',
            componentId: 'NavbarDefault',
            props: {
              brandText: 'Nexus Solutions',
              navItems: [
                { label: 'Home', href: '/', active: true },
                { label: 'Services', href: '/services', active: false },
                { label: 'Case Studies', href: '/cases', active: false },
                { label: 'About', href: '/about', active: false },
                { label: 'Contact', href: '/contact', active: false },
              ],
            },
            styles: {
              backgroundColor: '#ffffff',
              boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
            },
          },
          // Hero Section
          {
            pluginId: 'core-ui',
            componentId: 'Container',
            props: {
              layoutMode: 'flex-column',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '24px',
            },
            styles: {
              backgroundColor: '#1a1a2e',
              padding: '80px 40px',
              textAlign: 'center',
              minHeight: '500px',
              width: '100%',
            },
            children: [
              {
                pluginId: 'core-ui',
                componentId: 'Label',
                props: { text: 'Transform Your Business with Digital Innovation', htmlTag: 'h1' },
                styles: { fontSize: '48px', fontWeight: '700', color: '#ffffff', marginBottom: '16px', maxWidth: '900px', textAlign: 'center' },
              },
              {
                pluginId: 'core-ui',
                componentId: 'Label',
                props: { text: 'We partner with forward-thinking companies to build scalable software solutions, streamline operations, and accelerate growth through cutting-edge technology.', htmlTag: 'p' },
                styles: { fontSize: '18px', color: 'rgba(255,255,255,0.8)', maxWidth: '700px', lineHeight: '1.6', textAlign: 'center' },
              },
              {
                pluginId: 'core-ui',
                componentId: 'Container',
                props: { layoutMode: 'flex-row', gap: '16px', justifyContent: 'center' },
                styles: { marginTop: '32px' },
                children: [
                  {
                    pluginId: 'core-ui',
                    componentId: 'Button',
                    props: { text: 'Schedule a Consultation', variant: 'primary' },
                    styles: { padding: '16px 32px', fontSize: '16px', backgroundColor: '#007bff', borderRadius: '8px' },
                  },
                  {
                    pluginId: 'core-ui',
                    componentId: 'Button',
                    props: { text: 'View Our Work', variant: 'outline' },
                    styles: { padding: '16px 32px', fontSize: '16px', borderColor: '#ffffff', color: '#ffffff', borderRadius: '8px', border: '2px solid #ffffff', backgroundColor: 'transparent' },
                  },
                ],
              },
            ],
          },
          // Features Section
          {
            pluginId: 'core-ui',
            componentId: 'Container',
            props: { layoutMode: 'flex-column', alignItems: 'center', gap: '48px' },
            styles: { padding: '80px 40px', backgroundColor: '#ffffff', width: '100%' },
            children: [
              {
                pluginId: 'core-ui',
                componentId: 'Label',
                props: { text: 'Our Core Services', htmlTag: 'h2' },
                styles: { fontSize: '36px', fontWeight: '700', color: '#212529', textAlign: 'center' },
              },
              {
                pluginId: 'core-ui',
                componentId: 'Container',
                props: { layoutMode: 'flex-row', gap: '32px', justifyContent: 'center' },
                styles: { width: '100%', maxWidth: '1200px', flexWrap: 'wrap' },
                children: [
                  // Feature Card 1
                  {
                    pluginId: 'core-ui',
                    componentId: 'Container',
                    props: { layoutMode: 'flex-column', alignItems: 'center', gap: '16px' },
                    styles: { flex: '1', minWidth: '280px', maxWidth: '350px', padding: '32px', backgroundColor: '#f8f9fa', borderRadius: '12px', textAlign: 'center' },
                    children: [
                      { pluginId: 'core-ui', componentId: 'Label', props: { text: '💻', htmlTag: 'span' }, styles: { fontSize: '48px' } },
                      { pluginId: 'core-ui', componentId: 'Label', props: { text: 'Custom Software Development', htmlTag: 'h3' }, styles: { fontSize: '20px', fontWeight: '600', color: '#212529', textAlign: 'center' } },
                      { pluginId: 'core-ui', componentId: 'Label', props: { text: 'Tailored applications built from the ground up to solve your unique business challenges and drive efficiency.', htmlTag: 'p' }, styles: { fontSize: '14px', color: '#6c757d', lineHeight: '1.6', textAlign: 'center' } },
                    ],
                  },
                  // Feature Card 2
                  {
                    pluginId: 'core-ui',
                    componentId: 'Container',
                    props: { layoutMode: 'flex-column', alignItems: 'center', gap: '16px' },
                    styles: { flex: '1', minWidth: '280px', maxWidth: '350px', padding: '32px', backgroundColor: '#f8f9fa', borderRadius: '12px', textAlign: 'center' },
                    children: [
                      { pluginId: 'core-ui', componentId: 'Label', props: { text: '☁️', htmlTag: 'span' }, styles: { fontSize: '48px' } },
                      { pluginId: 'core-ui', componentId: 'Label', props: { text: 'Cloud Infrastructure', htmlTag: 'h3' }, styles: { fontSize: '20px', fontWeight: '600', color: '#212529', textAlign: 'center' } },
                      { pluginId: 'core-ui', componentId: 'Label', props: { text: 'Migrate to the cloud with confidence. We design, deploy, and manage scalable AWS, Azure, and GCP solutions.', htmlTag: 'p' }, styles: { fontSize: '14px', color: '#6c757d', lineHeight: '1.6', textAlign: 'center' } },
                    ],
                  },
                  // Feature Card 3
                  {
                    pluginId: 'core-ui',
                    componentId: 'Container',
                    props: { layoutMode: 'flex-column', alignItems: 'center', gap: '16px' },
                    styles: { flex: '1', minWidth: '280px', maxWidth: '350px', padding: '32px', backgroundColor: '#f8f9fa', borderRadius: '12px', textAlign: 'center' },
                    children: [
                      { pluginId: 'core-ui', componentId: 'Label', props: { text: '📊', htmlTag: 'span' }, styles: { fontSize: '48px' } },
                      { pluginId: 'core-ui', componentId: 'Label', props: { text: 'Data Analytics & AI', htmlTag: 'h3' }, styles: { fontSize: '20px', fontWeight: '600', color: '#212529', textAlign: 'center' } },
                      { pluginId: 'core-ui', componentId: 'Label', props: { text: 'Unlock insights from your data with advanced analytics, machine learning models, and intelligent automation.', htmlTag: 'p' }, styles: { fontSize: '14px', color: '#6c757d', lineHeight: '1.6', textAlign: 'center' } },
                    ],
                  },
                ],
              },
            ],
          },
          // Stats Section
          {
            pluginId: 'core-ui',
            componentId: 'Container',
            props: { layoutMode: 'flex-row', alignItems: 'center', justifyContent: 'space-around' },
            styles: { padding: '60px 40px', backgroundColor: '#007bff', width: '100%', flexWrap: 'wrap', gap: '32px' },
            children: [
              {
                pluginId: 'core-ui',
                componentId: 'Container',
                props: { layoutMode: 'flex-column', alignItems: 'center', gap: '8px' },
                styles: { textAlign: 'center', minWidth: '150px' },
                children: [
                  { pluginId: 'core-ui', componentId: 'Label', props: { text: '500+', htmlTag: 'span' }, styles: { fontSize: '42px', fontWeight: '700', color: '#ffffff', textAlign: 'center' } },
                  { pluginId: 'core-ui', componentId: 'Label', props: { text: 'Projects Delivered', htmlTag: 'p' }, styles: { fontSize: '14px', color: 'rgba(255,255,255,0.8)', textAlign: 'center' } },
                ],
              },
              {
                pluginId: 'core-ui',
                componentId: 'Container',
                props: { layoutMode: 'flex-column', alignItems: 'center', gap: '8px' },
                styles: { textAlign: 'center', minWidth: '150px' },
                children: [
                  { pluginId: 'core-ui', componentId: 'Label', props: { text: '98%', htmlTag: 'span' }, styles: { fontSize: '42px', fontWeight: '700', color: '#ffffff', textAlign: 'center' } },
                  { pluginId: 'core-ui', componentId: 'Label', props: { text: 'Client Retention', htmlTag: 'p' }, styles: { fontSize: '14px', color: 'rgba(255,255,255,0.8)', textAlign: 'center' } },
                ],
              },
              {
                pluginId: 'core-ui',
                componentId: 'Container',
                props: { layoutMode: 'flex-column', alignItems: 'center', gap: '8px' },
                styles: { textAlign: 'center', minWidth: '150px' },
                children: [
                  { pluginId: 'core-ui', componentId: 'Label', props: { text: '12', htmlTag: 'span' }, styles: { fontSize: '42px', fontWeight: '700', color: '#ffffff', textAlign: 'center' } },
                  { pluginId: 'core-ui', componentId: 'Label', props: { text: 'Years in Business', htmlTag: 'p' }, styles: { fontSize: '14px', color: 'rgba(255,255,255,0.8)', textAlign: 'center' } },
                ],
              },
              {
                pluginId: 'core-ui',
                componentId: 'Container',
                props: { layoutMode: 'flex-column', alignItems: 'center', gap: '8px' },
                styles: { textAlign: 'center', minWidth: '150px' },
                children: [
                  { pluginId: 'core-ui', componentId: 'Label', props: { text: '45+', htmlTag: 'span' }, styles: { fontSize: '42px', fontWeight: '700', color: '#ffffff', textAlign: 'center' } },
                  { pluginId: 'core-ui', componentId: 'Label', props: { text: 'Expert Engineers', htmlTag: 'p' }, styles: { fontSize: '14px', color: 'rgba(255,255,255,0.8)', textAlign: 'center' } },
                ],
              },
            ],
          },
          // Testimonial Section
          {
            pluginId: 'core-ui',
            componentId: 'Container',
            props: { layoutMode: 'flex-column', alignItems: 'center', gap: '32px' },
            styles: { padding: '80px 40px', backgroundColor: '#ffffff', textAlign: 'center', width: '100%' },
            children: [
              { pluginId: 'core-ui', componentId: 'Label', props: { text: 'What Our Clients Say', htmlTag: 'h2' }, styles: { fontSize: '36px', fontWeight: '700', color: '#212529', textAlign: 'center' } },
              {
                pluginId: 'core-ui',
                componentId: 'Container',
                props: { layoutMode: 'flex-column', alignItems: 'center', gap: '20px' },
                styles: { maxWidth: '700px', padding: '40px', backgroundColor: '#f8f9fa', borderRadius: '16px' },
                children: [
                  { pluginId: 'core-ui', componentId: 'Label', props: { text: '"Nexus Solutions transformed our legacy systems into a modern, cloud-native platform. Their team\'s expertise and dedication exceeded our expectations. We saw a 40% improvement in operational efficiency within the first quarter."', htmlTag: 'p' }, styles: { fontSize: '18px', color: '#495057', lineHeight: '1.8', fontStyle: 'italic', textAlign: 'center' } },
                  { pluginId: 'core-ui', componentId: 'Label', props: { text: '— Sarah Mitchell, CTO at TechFlow Industries', htmlTag: 'p' }, styles: { fontSize: '14px', color: '#6c757d', fontWeight: '600', textAlign: 'center' } },
                ],
              },
            ],
          },
          // CTA Section
          {
            pluginId: 'core-ui',
            componentId: 'Container',
            props: { layoutMode: 'flex-column', alignItems: 'center', gap: '24px' },
            styles: { padding: '80px 40px', backgroundColor: '#f8f9fa', textAlign: 'center', width: '100%' },
            children: [
              { pluginId: 'core-ui', componentId: 'Label', props: { text: 'Ready to Accelerate Your Digital Transformation?', htmlTag: 'h2' }, styles: { fontSize: '36px', fontWeight: '700', color: '#212529', textAlign: 'center' } },
              { pluginId: 'core-ui', componentId: 'Label', props: { text: 'Schedule a free 30-minute strategy session with our experts and discover how we can help you achieve your business goals.', htmlTag: 'p' }, styles: { fontSize: '18px', color: '#6c757d', maxWidth: '600px', textAlign: 'center' } },
              { pluginId: 'core-ui', componentId: 'Button', props: { text: 'Book Your Free Consultation', variant: 'primary' }, styles: { padding: '16px 40px', fontSize: '18px', borderRadius: '8px' } },
            ],
          },
          // Footer
          {
            pluginId: 'core-ui',
            componentId: 'Container',
            props: { layoutMode: 'flex-row', alignItems: 'center', justifyContent: 'space-between' },
            styles: { padding: '24px 40px', backgroundColor: '#212529', width: '100%', flexWrap: 'wrap', gap: '16px' },
            children: [
              { pluginId: 'core-ui', componentId: 'Label', props: { text: '© 2024 Nexus Solutions Inc. All rights reserved.', htmlTag: 'p' }, styles: { fontSize: '14px', color: 'rgba(255,255,255,0.7)' } },
              {
                pluginId: 'core-ui',
                componentId: 'Container',
                props: { layoutMode: 'flex-row', gap: '24px' },
                styles: {},
                children: [
                  { pluginId: 'core-ui', componentId: 'Label', props: { text: 'Privacy Policy', htmlTag: 'a' }, styles: { fontSize: '14px', color: 'rgba(255,255,255,0.7)', cursor: 'pointer' } },
                  { pluginId: 'core-ui', componentId: 'Label', props: { text: 'Terms of Service', htmlTag: 'a' }, styles: { fontSize: '14px', color: 'rgba(255,255,255,0.7)', cursor: 'pointer' } },
                  { pluginId: 'core-ui', componentId: 'Label', props: { text: 'Careers', htmlTag: 'a' }, styles: { fontSize: '14px', color: 'rgba(255,255,255,0.7)', cursor: 'pointer' } },
                ],
              },
            ],
          },
        ],
      },
    ],
  },
  {
    id: 'homepage-startup',
    name: 'Startup Landing Page',
    category: 'hero',
    icon: '🚀',
    description: 'Modern startup landing page with gradient hero, feature list, and email signup',
    tags: ['homepage', 'startup', 'landing', 'modern'],
    suggestedSize: { columnSpan: 12, rowSpan: 1 },
    components: [
      // Single root container
      {
        pluginId: 'core-ui',
        componentId: 'Container',
        props: { layoutMode: 'flex-column', gap: '0px' },
        styles: { width: '100%', minHeight: '100vh' },
        children: [
          // Navbar
          {
            pluginId: 'core-navbar',
            componentId: 'NavbarMinimal',
            props: {
              brandText: 'LaunchPad',
              navItems: [
                { label: 'Features', href: '#features', active: false },
                { label: 'Pricing', href: '#pricing', active: false },
                { label: 'About', href: '#about', active: false },
                { label: 'Get Started', href: '#signup', active: true },
              ],
            },
            styles: { backgroundColor: 'transparent' },
          },
          // Hero Section with Gradient
          {
            pluginId: 'core-ui',
            componentId: 'Container',
            props: { layoutMode: 'flex-column', alignItems: 'center', justifyContent: 'center', gap: '32px' },
            styles: {
              background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
              padding: '120px 40px',
              minHeight: '600px',
              width: '100%',
            },
            children: [
              { pluginId: 'core-ui', componentId: 'Label', props: { text: '🚀', htmlTag: 'span' }, styles: { fontSize: '64px' } },
              { pluginId: 'core-ui', componentId: 'Label', props: { text: 'Launch Your Ideas Into Reality', htmlTag: 'h1' }, styles: { fontSize: '52px', fontWeight: '800', color: '#ffffff', textAlign: 'center', maxWidth: '800px' } },
              { pluginId: 'core-ui', componentId: 'Label', props: { text: 'The all-in-one platform for entrepreneurs and startups to build, launch, and scale their products faster than ever before.', htmlTag: 'p' }, styles: { fontSize: '20px', color: 'rgba(255,255,255,0.9)', textAlign: 'center', maxWidth: '600px', lineHeight: '1.6' } },
              {
                pluginId: 'core-ui',
                componentId: 'Container',
                props: { layoutMode: 'flex-row', gap: '12px', alignItems: 'center', justifyContent: 'center' },
                styles: { marginTop: '24px', flexWrap: 'wrap' },
                children: [
                  { pluginId: 'core-ui', componentId: 'Textbox', props: { placeholder: 'Enter your email address', type: 'email' }, styles: { padding: '16px 20px', fontSize: '16px', borderRadius: '8px', border: 'none', minWidth: '300px', backgroundColor: '#ffffff' } },
                  { pluginId: 'core-ui', componentId: 'Button', props: { text: 'Get Early Access', variant: 'primary' }, styles: { padding: '16px 32px', fontSize: '16px', backgroundColor: '#1a1a2e', borderRadius: '8px' } },
                ],
              },
              { pluginId: 'core-ui', componentId: 'Label', props: { text: 'Join 2,500+ founders already on the waitlist', htmlTag: 'p' }, styles: { fontSize: '14px', color: 'rgba(255,255,255,0.7)', marginTop: '16px' } },
            ],
          },
          // Features Section
          {
            pluginId: 'core-ui',
            componentId: 'Container',
            props: { layoutMode: 'flex-column', alignItems: 'center', gap: '48px' },
            styles: { padding: '100px 40px', backgroundColor: '#ffffff', width: '100%' },
            children: [
              { pluginId: 'core-ui', componentId: 'Label', props: { text: 'Everything You Need to Succeed', htmlTag: 'h2' }, styles: { fontSize: '36px', fontWeight: '700', color: '#212529', textAlign: 'center' } },
              { pluginId: 'core-ui', componentId: 'Label', props: { text: 'Powerful features designed to help you move fast and stay focused on what matters', htmlTag: 'p' }, styles: { fontSize: '18px', color: '#6c757d', textAlign: 'center', maxWidth: '600px' } },
              {
                pluginId: 'core-ui',
                componentId: 'Container',
                props: { layoutMode: 'flex-row', gap: '32px', justifyContent: 'center' },
                styles: { width: '100%', maxWidth: '1200px', flexWrap: 'wrap' },
                children: [
                  {
                    pluginId: 'core-ui',
                    componentId: 'Container',
                    props: { layoutMode: 'flex-column', alignItems: 'center', gap: '16px' },
                    styles: { flex: '1', minWidth: '250px', maxWidth: '300px', padding: '32px', textAlign: 'center' },
                    children: [
                      { pluginId: 'core-ui', componentId: 'Label', props: { text: '⚡', htmlTag: 'span' }, styles: { fontSize: '40px' } },
                      { pluginId: 'core-ui', componentId: 'Label', props: { text: 'Lightning Fast', htmlTag: 'h3' }, styles: { fontSize: '20px', fontWeight: '600', color: '#212529' } },
                      { pluginId: 'core-ui', componentId: 'Label', props: { text: 'Deploy in seconds with our optimized infrastructure', htmlTag: 'p' }, styles: { fontSize: '14px', color: '#6c757d', lineHeight: '1.5' } },
                    ],
                  },
                  {
                    pluginId: 'core-ui',
                    componentId: 'Container',
                    props: { layoutMode: 'flex-column', alignItems: 'center', gap: '16px' },
                    styles: { flex: '1', minWidth: '250px', maxWidth: '300px', padding: '32px', textAlign: 'center' },
                    children: [
                      { pluginId: 'core-ui', componentId: 'Label', props: { text: '🔒', htmlTag: 'span' }, styles: { fontSize: '40px' } },
                      { pluginId: 'core-ui', componentId: 'Label', props: { text: 'Secure by Default', htmlTag: 'h3' }, styles: { fontSize: '20px', fontWeight: '600', color: '#212529' } },
                      { pluginId: 'core-ui', componentId: 'Label', props: { text: 'Enterprise-grade security built into every layer', htmlTag: 'p' }, styles: { fontSize: '14px', color: '#6c757d', lineHeight: '1.5' } },
                    ],
                  },
                  {
                    pluginId: 'core-ui',
                    componentId: 'Container',
                    props: { layoutMode: 'flex-column', alignItems: 'center', gap: '16px' },
                    styles: { flex: '1', minWidth: '250px', maxWidth: '300px', padding: '32px', textAlign: 'center' },
                    children: [
                      { pluginId: 'core-ui', componentId: 'Label', props: { text: '📈', htmlTag: 'span' }, styles: { fontSize: '40px' } },
                      { pluginId: 'core-ui', componentId: 'Label', props: { text: 'Scale Infinitely', htmlTag: 'h3' }, styles: { fontSize: '20px', fontWeight: '600', color: '#212529' } },
                      { pluginId: 'core-ui', componentId: 'Label', props: { text: 'Grow from MVP to millions of users seamlessly', htmlTag: 'p' }, styles: { fontSize: '14px', color: '#6c757d', lineHeight: '1.5' } },
                    ],
                  },
                ],
              },
            ],
          },
          // CTA Section
          {
            pluginId: 'core-ui',
            componentId: 'Container',
            props: { layoutMode: 'flex-column', alignItems: 'center', gap: '24px' },
            styles: { padding: '80px 40px', background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)', width: '100%' },
            children: [
              { pluginId: 'core-ui', componentId: 'Label', props: { text: 'Ready to Launch?', htmlTag: 'h2' }, styles: { fontSize: '36px', fontWeight: '700', color: '#ffffff', textAlign: 'center' } },
              { pluginId: 'core-ui', componentId: 'Label', props: { text: 'Start building for free today. No credit card required.', htmlTag: 'p' }, styles: { fontSize: '18px', color: 'rgba(255,255,255,0.9)', textAlign: 'center' } },
              { pluginId: 'core-ui', componentId: 'Button', props: { text: 'Start Free Trial', variant: 'primary' }, styles: { padding: '16px 48px', fontSize: '18px', backgroundColor: '#ffffff', color: '#667eea', borderRadius: '8px', fontWeight: '600' } },
            ],
          },
          // Footer
          {
            pluginId: 'core-ui',
            componentId: 'Container',
            props: { layoutMode: 'flex-column', alignItems: 'center', gap: '16px' },
            styles: { padding: '40px', backgroundColor: '#1a1a2e', width: '100%' },
            children: [
              { pluginId: 'core-ui', componentId: 'Label', props: { text: 'LaunchPad', htmlTag: 'p' }, styles: { fontSize: '20px', fontWeight: '700', color: '#ffffff' } },
              { pluginId: 'core-ui', componentId: 'Label', props: { text: '© 2024 LaunchPad Inc. All rights reserved.', htmlTag: 'p' }, styles: { fontSize: '14px', color: 'rgba(255,255,255,0.6)' } },
            ],
          },
        ],
      },
    ],
  },
];

/**
 * About Page Templates
 */
const aboutTemplates: UITemplate[] = [
  {
    id: 'about-personal',
    name: 'Personal About Me',
    category: 'content',
    icon: '👤',
    description: 'Personal about me page with bio, skills, and contact CTA',
    tags: ['about', 'personal', 'portfolio', 'bio'],
    suggestedSize: { columnSpan: 12, rowSpan: 1 },
    components: [
      {
        pluginId: 'core-ui',
        componentId: 'Container',
        props: { layoutMode: 'flex-column', gap: '0px' },
        styles: { width: '100%', minHeight: '100vh' },
        children: [
          // Navbar
          {
            pluginId: 'core-navbar',
            componentId: 'NavbarMinimal',
            props: {
              brandText: 'Alex Chen',
              navItems: [
                { label: 'About', href: '#about', active: true },
                { label: 'Skills', href: '#skills', active: false },
                { label: 'Projects', href: '#projects', active: false },
                { label: 'Contact', href: '#contact', active: false },
              ],
            },
            styles: { backgroundColor: '#ffffff' },
          },
          // Hero/Intro Section
          {
            pluginId: 'core-ui',
            componentId: 'Container',
            props: { layoutMode: 'flex-column', alignItems: 'center', justifyContent: 'center', gap: '24px' },
            styles: { padding: '100px 40px', backgroundColor: '#f8f9fa', width: '100%' },
            children: [
              { pluginId: 'core-ui', componentId: 'Image', props: { src: '', alt: 'Alex Chen - Profile Photo', aspectRatio: 'circle', placeholderColor: '#dee2e6' }, styles: { width: '180px', height: '180px', boxShadow: '0 4px 20px rgba(0,0,0,0.15)' } },
              { pluginId: 'core-ui', componentId: 'Label', props: { text: 'Hi, I\'m Alex Chen', htmlTag: 'h1' }, styles: { fontSize: '48px', fontWeight: '700', color: '#212529', textAlign: 'center', marginTop: '16px' } },
              { pluginId: 'core-ui', componentId: 'Label', props: { text: 'Full-Stack Developer & UI/UX Designer', htmlTag: 'p' }, styles: { fontSize: '24px', color: '#6c757d', textAlign: 'center' } },
              { pluginId: 'core-ui', componentId: 'Label', props: { text: 'I craft beautiful digital experiences that delight users and drive business results. With 8+ years of experience, I specialize in building scalable web applications and intuitive user interfaces.', htmlTag: 'p' }, styles: { fontSize: '18px', color: '#495057', textAlign: 'center', maxWidth: '700px', lineHeight: '1.7' } },
            ],
          },
          // Skills Section
          {
            pluginId: 'core-ui',
            componentId: 'Container',
            props: { layoutMode: 'flex-column', alignItems: 'center', gap: '48px' },
            styles: { padding: '80px 40px', backgroundColor: '#ffffff', width: '100%' },
            children: [
              { pluginId: 'core-ui', componentId: 'Label', props: { text: 'What I Do', htmlTag: 'h2' }, styles: { fontSize: '36px', fontWeight: '700', color: '#212529', textAlign: 'center' } },
              {
                pluginId: 'core-ui',
                componentId: 'Container',
                props: { layoutMode: 'flex-row', gap: '32px', justifyContent: 'center' },
                styles: { width: '100%', maxWidth: '1000px', flexWrap: 'wrap' },
                children: [
                  {
                    pluginId: 'core-ui',
                    componentId: 'Container',
                    props: { layoutMode: 'flex-column', alignItems: 'center', gap: '12px' },
                    styles: { flex: '1', minWidth: '200px', padding: '24px', backgroundColor: '#f8f9fa', borderRadius: '12px', textAlign: 'center' },
                    children: [
                      { pluginId: 'core-ui', componentId: 'Label', props: { text: '🎨', htmlTag: 'span' }, styles: { fontSize: '36px' } },
                      { pluginId: 'core-ui', componentId: 'Label', props: { text: 'UI/UX Design', htmlTag: 'h3' }, styles: { fontSize: '18px', fontWeight: '600', color: '#212529' } },
                      { pluginId: 'core-ui', componentId: 'Label', props: { text: 'Figma, Sketch, Adobe XD', htmlTag: 'p' }, styles: { fontSize: '14px', color: '#6c757d' } },
                    ],
                  },
                  {
                    pluginId: 'core-ui',
                    componentId: 'Container',
                    props: { layoutMode: 'flex-column', alignItems: 'center', gap: '12px' },
                    styles: { flex: '1', minWidth: '200px', padding: '24px', backgroundColor: '#f8f9fa', borderRadius: '12px', textAlign: 'center' },
                    children: [
                      { pluginId: 'core-ui', componentId: 'Label', props: { text: '💻', htmlTag: 'span' }, styles: { fontSize: '36px' } },
                      { pluginId: 'core-ui', componentId: 'Label', props: { text: 'Frontend Dev', htmlTag: 'h3' }, styles: { fontSize: '18px', fontWeight: '600', color: '#212529' } },
                      { pluginId: 'core-ui', componentId: 'Label', props: { text: 'React, Vue, TypeScript', htmlTag: 'p' }, styles: { fontSize: '14px', color: '#6c757d' } },
                    ],
                  },
                  {
                    pluginId: 'core-ui',
                    componentId: 'Container',
                    props: { layoutMode: 'flex-column', alignItems: 'center', gap: '12px' },
                    styles: { flex: '1', minWidth: '200px', padding: '24px', backgroundColor: '#f8f9fa', borderRadius: '12px', textAlign: 'center' },
                    children: [
                      { pluginId: 'core-ui', componentId: 'Label', props: { text: '⚙️', htmlTag: 'span' }, styles: { fontSize: '36px' } },
                      { pluginId: 'core-ui', componentId: 'Label', props: { text: 'Backend Dev', htmlTag: 'h3' }, styles: { fontSize: '18px', fontWeight: '600', color: '#212529' } },
                      { pluginId: 'core-ui', componentId: 'Label', props: { text: 'Node.js, Python, PostgreSQL', htmlTag: 'p' }, styles: { fontSize: '14px', color: '#6c757d' } },
                    ],
                  },
                ],
              },
            ],
          },
          // CTA Section
          {
            pluginId: 'core-ui',
            componentId: 'Container',
            props: { layoutMode: 'flex-column', alignItems: 'center', gap: '24px' },
            styles: { padding: '80px 40px', backgroundColor: '#212529', width: '100%' },
            children: [
              { pluginId: 'core-ui', componentId: 'Label', props: { text: 'Let\'s Work Together', htmlTag: 'h2' }, styles: { fontSize: '36px', fontWeight: '700', color: '#ffffff', textAlign: 'center' } },
              { pluginId: 'core-ui', componentId: 'Label', props: { text: 'I\'m always open to discussing new projects and opportunities.', htmlTag: 'p' }, styles: { fontSize: '18px', color: 'rgba(255,255,255,0.8)', textAlign: 'center' } },
              { pluginId: 'core-ui', componentId: 'Button', props: { text: 'Get In Touch', variant: 'primary' }, styles: { padding: '16px 40px', fontSize: '18px', borderRadius: '8px' } },
            ],
          },
          // Footer
          {
            pluginId: 'core-ui',
            componentId: 'Container',
            props: { layoutMode: 'flex-column', alignItems: 'center', gap: '8px' },
            styles: { padding: '24px 40px', backgroundColor: '#1a1a2e', width: '100%' },
            children: [
              { pluginId: 'core-ui', componentId: 'Label', props: { text: '© 2024 Alex Chen. Made with ❤️', htmlTag: 'p' }, styles: { fontSize: '14px', color: 'rgba(255,255,255,0.6)' } },
            ],
          },
        ],
      },
    ],
  },
];

/**
 * Portfolio Templates
 */
const portfolioTemplates: UITemplate[] = [
  {
    id: 'portfolio-creative',
    name: 'Creative Portfolio',
    category: 'gallery',
    icon: '🎨',
    description: 'Creative portfolio with project showcase grid',
    tags: ['portfolio', 'creative', 'designer', 'projects'],
    suggestedSize: { columnSpan: 12, rowSpan: 1 },
    components: [
      {
        pluginId: 'core-ui',
        componentId: 'Container',
        props: { layoutMode: 'flex-column', gap: '0px' },
        styles: { width: '100%', minHeight: '100vh' },
        children: [
          // Navbar
          {
            pluginId: 'core-navbar',
            componentId: 'NavbarMinimal',
            props: {
              brandText: 'Studio X',
              navItems: [
                { label: 'Work', href: '#work', active: true },
                { label: 'Services', href: '#services', active: false },
                { label: 'About', href: '#about', active: false },
                { label: 'Contact', href: '#contact', active: false },
              ],
            },
            styles: { backgroundColor: '#ffffff' },
          },
          // Hero
          {
            pluginId: 'core-ui',
            componentId: 'Container',
            props: { layoutMode: 'flex-column', alignItems: 'center', justifyContent: 'center', gap: '24px' },
            styles: { padding: '100px 40px', backgroundColor: '#000000', width: '100%' },
            children: [
              { pluginId: 'core-ui', componentId: 'Label', props: { text: 'We Create Digital Experiences', htmlTag: 'h1' }, styles: { fontSize: '56px', fontWeight: '800', color: '#ffffff', textAlign: 'center', maxWidth: '800px' } },
              { pluginId: 'core-ui', componentId: 'Label', props: { text: 'Award-winning design studio specializing in brand identity, web design, and digital products', htmlTag: 'p' }, styles: { fontSize: '20px', color: 'rgba(255,255,255,0.7)', textAlign: 'center', maxWidth: '600px' } },
            ],
          },
          // Projects Grid
          {
            pluginId: 'core-ui',
            componentId: 'Container',
            props: { layoutMode: 'flex-column', alignItems: 'center', gap: '48px' },
            styles: { padding: '80px 40px', backgroundColor: '#ffffff', width: '100%' },
            children: [
              { pluginId: 'core-ui', componentId: 'Label', props: { text: 'Selected Work', htmlTag: 'h2' }, styles: { fontSize: '36px', fontWeight: '700', color: '#212529', textAlign: 'center' } },
              {
                pluginId: 'core-ui',
                componentId: 'Container',
                props: { layoutMode: 'flex-row', gap: '24px', justifyContent: 'center' },
                styles: { width: '100%', maxWidth: '1200px', flexWrap: 'wrap' },
                children: [
                  // Project 1
                  {
                    pluginId: 'core-ui',
                    componentId: 'Container',
                    props: { layoutMode: 'flex-column', gap: '16px' },
                    styles: { flex: '1', minWidth: '300px', maxWidth: '380px' },
                    children: [
                      { pluginId: 'core-ui', componentId: 'Image', props: { src: '', alt: 'Nike Brand Redesign', aspectRatio: '4:3', placeholderColor: '#e9ecef', borderRadius: '12px' }, styles: { width: '100%' } },
                      { pluginId: 'core-ui', componentId: 'Label', props: { text: 'Brand Redesign — Nike', htmlTag: 'h3' }, styles: { fontSize: '18px', fontWeight: '600', color: '#212529' } },
                      { pluginId: 'core-ui', componentId: 'Label', props: { text: 'Branding / Visual Identity', htmlTag: 'p' }, styles: { fontSize: '14px', color: '#6c757d' } },
                    ],
                  },
                  // Project 2
                  {
                    pluginId: 'core-ui',
                    componentId: 'Container',
                    props: { layoutMode: 'flex-column', gap: '16px' },
                    styles: { flex: '1', minWidth: '300px', maxWidth: '380px' },
                    children: [
                      { pluginId: 'core-ui', componentId: 'Image', props: { src: '', alt: 'Shopify E-commerce', aspectRatio: '4:3', placeholderColor: '#dee2e6', borderRadius: '12px' }, styles: { width: '100%' } },
                      { pluginId: 'core-ui', componentId: 'Label', props: { text: 'E-commerce Platform — Shopify', htmlTag: 'h3' }, styles: { fontSize: '18px', fontWeight: '600', color: '#212529' } },
                      { pluginId: 'core-ui', componentId: 'Label', props: { text: 'Web Design / UX', htmlTag: 'p' }, styles: { fontSize: '14px', color: '#6c757d' } },
                    ],
                  },
                  // Project 3
                  {
                    pluginId: 'core-ui',
                    componentId: 'Container',
                    props: { layoutMode: 'flex-column', gap: '16px' },
                    styles: { flex: '1', minWidth: '300px', maxWidth: '380px' },
                    children: [
                      { pluginId: 'core-ui', componentId: 'Image', props: { src: '', alt: 'Spotify Mobile App', aspectRatio: '4:3', placeholderColor: '#ced4da', borderRadius: '12px' }, styles: { width: '100%' } },
                      { pluginId: 'core-ui', componentId: 'Label', props: { text: 'Mobile App — Spotify', htmlTag: 'h3' }, styles: { fontSize: '18px', fontWeight: '600', color: '#212529' } },
                      { pluginId: 'core-ui', componentId: 'Label', props: { text: 'Product Design / UI', htmlTag: 'p' }, styles: { fontSize: '14px', color: '#6c757d' } },
                    ],
                  },
                ],
              },
              { pluginId: 'core-ui', componentId: 'Button', props: { text: 'View All Projects', variant: 'outline' }, styles: { padding: '14px 32px', fontSize: '16px', borderRadius: '8px', marginTop: '24px' } },
            ],
          },
          // Team Section
          {
            pluginId: 'core-ui',
            componentId: 'Container',
            props: { layoutMode: 'flex-column', alignItems: 'center', gap: '48px' },
            styles: { padding: '80px 40px', backgroundColor: '#f8f9fa', width: '100%' },
            children: [
              { pluginId: 'core-ui', componentId: 'Label', props: { text: 'Meet Our Team', htmlTag: 'h2' }, styles: { fontSize: '36px', fontWeight: '700', color: '#212529', textAlign: 'center' } },
              { pluginId: 'core-ui', componentId: 'Label', props: { text: 'The creative minds behind Studio X', htmlTag: 'p' }, styles: { fontSize: '18px', color: '#6c757d', textAlign: 'center', maxWidth: '600px' } },
              {
                pluginId: 'core-ui',
                componentId: 'Container',
                props: { layoutMode: 'flex-row', gap: '40px', justifyContent: 'center' },
                styles: { width: '100%', maxWidth: '1000px', flexWrap: 'wrap' },
                children: [
                  // CEO
                  {
                    pluginId: 'core-ui',
                    componentId: 'Container',
                    props: { layoutMode: 'flex-column', alignItems: 'center', gap: '16px' },
                    styles: { flex: '1', minWidth: '200px', maxWidth: '280px', textAlign: 'center' },
                    children: [
                      { pluginId: 'core-ui', componentId: 'Image', props: { src: '', alt: 'Sarah Mitchell - CEO', aspectRatio: 'circle', placeholderColor: '#dee2e6' }, styles: { width: '150px', height: '150px' } },
                      { pluginId: 'core-ui', componentId: 'Label', props: { text: 'Sarah Mitchell', htmlTag: 'h3' }, styles: { fontSize: '20px', fontWeight: '600', color: '#212529', textAlign: 'center' } },
                      { pluginId: 'core-ui', componentId: 'Label', props: { text: 'CEO & Founder', htmlTag: 'p' }, styles: { fontSize: '14px', color: '#007bff', fontWeight: '500', textAlign: 'center' } },
                      { pluginId: 'core-ui', componentId: 'Label', props: { text: '15+ years leading creative agencies. Previously at Pentagram and IDEO.', htmlTag: 'p' }, styles: { fontSize: '14px', color: '#6c757d', lineHeight: '1.5', textAlign: 'center' } },
                    ],
                  },
                  // CTO
                  {
                    pluginId: 'core-ui',
                    componentId: 'Container',
                    props: { layoutMode: 'flex-column', alignItems: 'center', gap: '16px' },
                    styles: { flex: '1', minWidth: '200px', maxWidth: '280px', textAlign: 'center' },
                    children: [
                      { pluginId: 'core-ui', componentId: 'Image', props: { src: '', alt: 'David Chen - CTO', aspectRatio: 'circle', placeholderColor: '#e9ecef' }, styles: { width: '150px', height: '150px' } },
                      { pluginId: 'core-ui', componentId: 'Label', props: { text: 'David Chen', htmlTag: 'h3' }, styles: { fontSize: '20px', fontWeight: '600', color: '#212529', textAlign: 'center' } },
                      { pluginId: 'core-ui', componentId: 'Label', props: { text: 'Chief Technology Officer', htmlTag: 'p' }, styles: { fontSize: '14px', color: '#007bff', fontWeight: '500', textAlign: 'center' } },
                      { pluginId: 'core-ui', componentId: 'Label', props: { text: 'Former Google engineer. Expert in scalable systems and emerging technologies.', htmlTag: 'p' }, styles: { fontSize: '14px', color: '#6c757d', lineHeight: '1.5', textAlign: 'center' } },
                    ],
                  },
                  // Lead Architect
                  {
                    pluginId: 'core-ui',
                    componentId: 'Container',
                    props: { layoutMode: 'flex-column', alignItems: 'center', gap: '16px' },
                    styles: { flex: '1', minWidth: '200px', maxWidth: '280px', textAlign: 'center' },
                    children: [
                      { pluginId: 'core-ui', componentId: 'Image', props: { src: '', alt: 'Emily Rodriguez - Lead Architect', aspectRatio: 'circle', placeholderColor: '#ced4da' }, styles: { width: '150px', height: '150px' } },
                      { pluginId: 'core-ui', componentId: 'Label', props: { text: 'Emily Rodriguez', htmlTag: 'h3' }, styles: { fontSize: '20px', fontWeight: '600', color: '#212529', textAlign: 'center' } },
                      { pluginId: 'core-ui', componentId: 'Label', props: { text: 'Lead Solution Architect', htmlTag: 'p' }, styles: { fontSize: '14px', color: '#007bff', fontWeight: '500', textAlign: 'center' } },
                      { pluginId: 'core-ui', componentId: 'Label', props: { text: 'AWS certified architect. Specializes in cloud-native and microservices design.', htmlTag: 'p' }, styles: { fontSize: '14px', color: '#6c757d', lineHeight: '1.5', textAlign: 'center' } },
                    ],
                  },
                ],
              },
            ],
          },
          // Footer
          {
            pluginId: 'core-ui',
            componentId: 'Container',
            props: { layoutMode: 'flex-row', alignItems: 'center', justifyContent: 'space-between' },
            styles: { padding: '40px', backgroundColor: '#000000', width: '100%', flexWrap: 'wrap', gap: '16px' },
            children: [
              { pluginId: 'core-ui', componentId: 'Label', props: { text: 'Studio X © 2024', htmlTag: 'p' }, styles: { fontSize: '14px', color: 'rgba(255,255,255,0.6)' } },
              { pluginId: 'core-ui', componentId: 'Label', props: { text: 'hello@studiox.design', htmlTag: 'p' }, styles: { fontSize: '14px', color: 'rgba(255,255,255,0.6)' } },
            ],
          },
        ],
      },
    ],
  },
];

/**
 * Contact Page Templates
 */
const contactTemplates: UITemplate[] = [
  {
    id: 'contact-simple',
    name: 'Contact Page',
    category: 'contact',
    icon: '📧',
    description: 'Clean contact page with form and contact information',
    tags: ['contact', 'form', 'simple', 'business'],
    suggestedSize: { columnSpan: 12, rowSpan: 1 },
    components: [
      {
        pluginId: 'core-ui',
        componentId: 'Container',
        props: { layoutMode: 'flex-column', gap: '0px' },
        styles: { width: '100%', minHeight: '100vh' },
        children: [
          // Navbar
          {
            pluginId: 'core-navbar',
            componentId: 'NavbarDefault',
            props: {
              brandText: 'Company',
              navItems: [
                { label: 'Home', href: '/', active: false },
                { label: 'About', href: '/about', active: false },
                { label: 'Services', href: '/services', active: false },
                { label: 'Contact', href: '/contact', active: true },
              ],
            },
            styles: { backgroundColor: '#ffffff' },
          },
          // Header
          {
            pluginId: 'core-ui',
            componentId: 'Container',
            props: { layoutMode: 'flex-column', alignItems: 'center', gap: '16px' },
            styles: { padding: '80px 40px', backgroundColor: '#f8f9fa', width: '100%' },
            children: [
              { pluginId: 'core-ui', componentId: 'Label', props: { text: 'Get In Touch', htmlTag: 'h1' }, styles: { fontSize: '48px', fontWeight: '700', color: '#212529', textAlign: 'center' } },
              { pluginId: 'core-ui', componentId: 'Label', props: { text: 'We\'d love to hear from you. Send us a message and we\'ll respond as soon as possible.', htmlTag: 'p' }, styles: { fontSize: '18px', color: '#6c757d', textAlign: 'center', maxWidth: '600px' } },
            ],
          },
          // Contact Content
          {
            pluginId: 'core-ui',
            componentId: 'Container',
            props: { layoutMode: 'flex-row', gap: '64px', justifyContent: 'center' },
            styles: { padding: '80px 40px', backgroundColor: '#ffffff', width: '100%', flexWrap: 'wrap' },
            children: [
              // Contact Form
              {
                pluginId: 'core-ui',
                componentId: 'Container',
                props: { layoutMode: 'flex-column', gap: '24px' },
                styles: { flex: '1', minWidth: '300px', maxWidth: '500px' },
                children: [
                  { pluginId: 'core-ui', componentId: 'Label', props: { text: 'Send us a message', htmlTag: 'h3' }, styles: { fontSize: '24px', fontWeight: '600', color: '#212529' } },
                  { pluginId: 'core-ui', componentId: 'Textbox', props: { placeholder: 'Your Name', type: 'text' }, styles: { padding: '14px 16px', fontSize: '16px', borderRadius: '8px', border: '1px solid #dee2e6', width: '100%' } },
                  { pluginId: 'core-ui', componentId: 'Textbox', props: { placeholder: 'Your Email', type: 'email' }, styles: { padding: '14px 16px', fontSize: '16px', borderRadius: '8px', border: '1px solid #dee2e6', width: '100%' } },
                  { pluginId: 'core-ui', componentId: 'Textbox', props: { placeholder: 'Your Message', type: 'text' }, styles: { padding: '14px 16px', fontSize: '16px', borderRadius: '8px', border: '1px solid #dee2e6', width: '100%', minHeight: '150px' } },
                  { pluginId: 'core-ui', componentId: 'Button', props: { text: 'Send Message', variant: 'primary' }, styles: { padding: '16px 32px', fontSize: '16px', borderRadius: '8px', width: '100%' } },
                ],
              },
              // Contact Info
              {
                pluginId: 'core-ui',
                componentId: 'Container',
                props: { layoutMode: 'flex-column', gap: '32px' },
                styles: { flex: '1', minWidth: '250px', maxWidth: '350px' },
                children: [
                  { pluginId: 'core-ui', componentId: 'Label', props: { text: 'Contact Information', htmlTag: 'h3' }, styles: { fontSize: '24px', fontWeight: '600', color: '#212529' } },
                  {
                    pluginId: 'core-ui',
                    componentId: 'Container',
                    props: { layoutMode: 'flex-column', gap: '8px' },
                    styles: {},
                    children: [
                      { pluginId: 'core-ui', componentId: 'Label', props: { text: '📍 Address', htmlTag: 'p' }, styles: { fontSize: '14px', fontWeight: '600', color: '#212529' } },
                      { pluginId: 'core-ui', componentId: 'Label', props: { text: '123 Business Street, Suite 100\nSan Francisco, CA 94102', htmlTag: 'p' }, styles: { fontSize: '14px', color: '#6c757d', lineHeight: '1.6' } },
                    ],
                  },
                  {
                    pluginId: 'core-ui',
                    componentId: 'Container',
                    props: { layoutMode: 'flex-column', gap: '8px' },
                    styles: {},
                    children: [
                      { pluginId: 'core-ui', componentId: 'Label', props: { text: '📧 Email', htmlTag: 'p' }, styles: { fontSize: '14px', fontWeight: '600', color: '#212529' } },
                      { pluginId: 'core-ui', componentId: 'Label', props: { text: 'contact@company.com', htmlTag: 'p' }, styles: { fontSize: '14px', color: '#6c757d' } },
                    ],
                  },
                  {
                    pluginId: 'core-ui',
                    componentId: 'Container',
                    props: { layoutMode: 'flex-column', gap: '8px' },
                    styles: {},
                    children: [
                      { pluginId: 'core-ui', componentId: 'Label', props: { text: '📞 Phone', htmlTag: 'p' }, styles: { fontSize: '14px', fontWeight: '600', color: '#212529' } },
                      { pluginId: 'core-ui', componentId: 'Label', props: { text: '+1 (555) 123-4567', htmlTag: 'p' }, styles: { fontSize: '14px', color: '#6c757d' } },
                    ],
                  },
                ],
              },
            ],
          },
          // Footer
          {
            pluginId: 'core-ui',
            componentId: 'Container',
            props: { layoutMode: 'flex-column', alignItems: 'center', gap: '8px' },
            styles: { padding: '24px 40px', backgroundColor: '#212529', width: '100%' },
            children: [
              { pluginId: 'core-ui', componentId: 'Label', props: { text: '© 2024 Company Inc. All rights reserved.', htmlTag: 'p' }, styles: { fontSize: '14px', color: 'rgba(255,255,255,0.6)' } },
            ],
          },
        ],
      },
    ],
  },
];

/**
 * Template Groups for the palette
 */
export const templateGroups: TemplateGroup[] = [
  { category: 'hero', displayName: 'Homepage', icon: '🏠', templates: homepageTemplates },
  { category: 'content', displayName: 'About Pages', icon: '👤', templates: aboutTemplates },
  { category: 'gallery', displayName: 'Portfolio', icon: '🎨', templates: portfolioTemplates },
  { category: 'contact', displayName: 'Contact', icon: '📧', templates: contactTemplates },
];

/**
 * Flat list of all templates
 */
export const allTemplates: UITemplate[] = [
  ...homepageTemplates,
  ...aboutTemplates,
  ...portfolioTemplates,
  ...contactTemplates,
];

/**
 * Search templates by query
 */
export const searchTemplates = (query: string): UITemplate[] => {
  const lowerQuery = query.toLowerCase();
  return allTemplates.filter(t =>
    t.name.toLowerCase().includes(lowerQuery) ||
    t.description.toLowerCase().includes(lowerQuery) ||
    t.tags.some(tag => tag.toLowerCase().includes(lowerQuery))
  );
};
